package ru.astrainteractive.messagebridge.messenger.telegram.di

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runInterruptible
import okhttp3.Credentials
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.command.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.command.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.event.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.internal.CappedBackOff
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramBEventConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramAuthorMapper
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class TelegramMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
    messageInterceptors: List<MessageInterceptor<Update>>,
) : Logger by JUtiltLogger("MessageBridge-TelegramMessengerModule") {

    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val resolved = Dns.SYSTEM.lookup(hostname)
            return resolved.filterIsInstance<Inet4Address>().ifEmpty { resolved }
        }
    }

    private fun createOkHttpClient(proxy: PluginConfiguration.Proxy?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .writeTimeout(WRITE_TIMEOUT.toJavaDuration())
            .readTimeout(READ_TIMEOUT.toJavaDuration())
            .pingInterval(PING_INTERVAL.toJavaDuration())
            .retryOnConnectionFailure(true)
            .dns(ipv4FirstDns)
        if (proxy != null) {
            builder
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
                .proxyAuthenticator { route, response ->
                    val requestBuilder = response.request.newBuilder()
                    if (route?.socketAddress?.hostString == proxy.host) {
                        val credential: String = Credentials.basic(proxy.username, proxy.password)
                        requestBuilder.header("Proxy-Authorization", credential)
                    }
                    requestBuilder.build()
                }
        }
        return builder.build()
    }

    private val okHttpClientFlow = coreModule.configKrate.cachedStateFlow
        .map { pluginConfiguration -> pluginConfiguration.tgConfig.proxy }
        .distinctUntilChanged()
        .flatMapLatest { proxy ->
            callbackFlow {
                val okHttpClient = createOkHttpClient(proxy)
                send(okHttpClient)

                awaitClose {
                    okHttpClient.dispatcher.cancelAll()
                    okHttpClient.dispatcher.executorService.shutdown()
                    okHttpClient.connectionPool.evictAll()
                    okHttpClient.cache?.close()
                }
            }
        }
        .shareIn(coreModule.ioScope, SharingStarted.Lazily, 1)

    private val telegramClientFlow = combine(
        flow = coreModule.configKrate.cachedStateFlow
            .map { pluginConfiguration -> pluginConfiguration.tgConfig }
            .distinctUntilChanged(),
        flow2 = okHttpClientFlow,
        transform = { tgConfig, okHttpClient ->
            val client = OkHttpTelegramClient(
                okHttpClient,
                tgConfig.token
            )
            client
        }
    ).shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    private val relayedMessageCache = TelegramRelayedMessageCache(
        capacity = RELAYED_MESSAGE_CACHE_CAPACITY
    )

    private val messageSender = TelegramMessageSender(
        telegramClientFlow = telegramClientFlow,
    )

    private val telegramMessageController = TelegramBEventConsumer(
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        messageSender = messageSender,
        relayedMessageCache = relayedMessageCache,
        bEventReceiver = bEventChannel,
    )

    val bEventConsumer: BEventConsumer = telegramMessageController

    private val authorMapper = TelegramAuthorMapper(
        translationKrate = coreModule.translationKrate,
    )

    private val replyMapper = TelegramReplyMapper(
        configKrate = coreModule.configKrate,
        authorMapper = authorMapper,
        relayedMessageCache = relayedMessageCache,
    )

    private val relevanceChecker = TelegramMessageRelevanceMapper(
        configKrate = coreModule.configKrate,
        clock = Clock.System,
    )

    private val messageValidator = TelegramMessageValidatorMapper(
        configKrate = coreModule.configKrate,
        authorMapper = authorMapper,
    )

    private val commandParser = TelegramCommandMapper()

    private val commandHandler = TelegramCommandHandler(
        messageSender = messageSender,
        platformServer = coreModule.platformServer,
        translationKrate = coreModule.translationKrate,
    )

    private val consumer = TelegramChatConsumer(
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        translationKrate = coreModule.translationKrate,
        relevanceChecker = relevanceChecker,
        validator = messageValidator,
        replyMapper = replyMapper,
        commandParser = commandParser,
        commandHandler = commandHandler,
        messageSender = messageSender,
        bEventConsumer = bEventChannel,
        messageInterceptors = messageInterceptors,
    )

    private val bridgeBotFlow = coreModule.configKrate
        .cachedStateFlow
        .map { tgConfig -> tgConfig.tgConfig }
        .distinctUntilChanged()
        .combine(okHttpClientFlow) { tgConfig, okHttpClient ->
            val registrationBackOff = CappedBackOff()
            channelFlow {
                val pollerExecutor = Executors.newSingleThreadScheduledExecutor()
                val tgLpApplication = TelegramBotsLongPollingApplication(
                    Supplier(::ObjectMapper),
                    Supplier { okHttpClient },
                    Supplier { pollerExecutor },
                    Supplier { CappedBackOff() }
                )
                runCatching { runInterruptible { tgLpApplication.registerBot(tgConfig.token, consumer) } }
                    .onFailure { pollerExecutor.shutdownNow() }
                    .getOrThrow()
                info { "#bridgeBotFlow loaded!" }
                send(tgLpApplication)
                awaitClose {
                    info { "#bridgeBotFlow closing TelegramBotsLongPollingApplication..." }
                    okHttpClient.dispatcher.cancelAll()
                    tgLpApplication.unregisterBot(tgConfig.token)
                    tgLpApplication.stop()
                    tgLpApplication.close()
                    pollerExecutor.shutdownNow()
                }
            }.retryWhen { throwable, _ ->
                if (throwable !is TelegramApiException) return@retryWhen false
                val retryDelay = registrationBackOff.nextBackOffMillis().milliseconds
                warn { "#bridgeBotFlow could not register bot, retrying in $retryDelay: ${throwable.message}" }
                delay(retryDelay)
                true
            }
        }
        .flatMapLatest { tgLpApplicationFlow -> tgLpApplicationFlow }
        .shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            telegramMessageController.cancel()
        }
    )

    private companion object {
        /** Spent on every unreachable address before the next one is tried. */
        val CONNECT_TIMEOUT = 10.seconds

        val WRITE_TIMEOUT = 70.seconds

        /** Must exceed the getUpdates timeout, which holds the connection open. */
        val READ_TIMEOUT = 100.seconds

        val PING_INTERVAL = 15.seconds

        const val RELAYED_MESSAGE_CACHE_CAPACITY = 1000
    }
}
