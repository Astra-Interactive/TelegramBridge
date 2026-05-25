package ru.astrainteractive.messagebridge.messenger.telegram.di

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramAuthorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramChatConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.events.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramBEventConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.messaging.TelegramMessageSender
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class TelegramMessengerModule(
    coreModule: CoreModule,
    onlinePlayersProvider: OnlinePlayersProvider,
    linkModule: LinkModule,
) : Logger by JUtiltLogger("MessageBridge-TelegramModule") {

    private val okHttpClient = coreModule.configKrate.cachedStateFlow
        .map { pluginConfiguration -> pluginConfiguration.tgConfig.proxy }
        .distinctUntilChanged()
        .map { proxy ->
            if (proxy == null) {
                OkHttpClient.Builder().build()
            } else {
                @Suppress("MagicNumber")
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(75, TimeUnit.SECONDS)
                    .pingInterval(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
                    .proxyAuthenticator { route, response ->
                        var builder = response.request.newBuilder()
                        if (route?.socketAddress?.hostString == proxy.host) {
                            val credential: String = Credentials.basic(proxy.username, proxy.password)
                            builder.header("Proxy-Authorization", credential)
                        }
                        builder.build()
                    }
                    .build()
            }
        }
        .shareIn(coreModule.ioScope, SharingStarted.Lazily, 1)

    private val telegramClientFlow = coreModule.configKrate
        .cachedStateFlow
        .map { tgConfig -> tgConfig.tgConfig }
        .distinctUntilChanged()
        .combine(okHttpClient) { tgConfig, okHttpClient ->
            OkHttpTelegramClient(
                okHttpClient,
                tgConfig.token
            )
        }
        .shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    private val telegramMessageController = TelegramBEventConsumer(
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate,
        telegramClientFlow = telegramClientFlow,
    )

    private val authorMapper = TelegramAuthorMapper()

    private val relevanceChecker = TelegramMessageRelevanceMapper(
        configKrate = coreModule.configKrate,
    )

    private val messageValidator = TelegramMessageValidatorMapper(
        configKrate = coreModule.configKrate,
        authorMapper = authorMapper,
    )

    private val commandParser = TelegramCommandMapper()

    private val messageSender = TelegramMessageSender(
        telegramClientFlow = telegramClientFlow,
    )

    private val commandHandler = TelegramCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = onlinePlayersProvider,
        linkApi = linkModule.linkApi,
        translationKrate = coreModule.translationKrate,
    )

    private val consumer = TelegramChatConsumer(
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        translationKrate = coreModule.translationKrate,
        relevanceChecker = relevanceChecker,
        validator = messageValidator,
        commandParser = commandParser,
        commandHandler = commandHandler,
        messageSender = messageSender,
    )

    private val bridgeBotFlow = coreModule.configKrate
        .cachedStateFlow
        .map { tgConfig -> tgConfig.tgConfig }
        .distinctUntilChanged()
        .combine(okHttpClient) { tgConfig, okHttpClient ->
            channelFlow {
                val tgLpApplication = TelegramBotsLongPollingApplication(
                    Supplier(::ObjectMapper),
                    Supplier { okHttpClient }
                )
                try {
                    tgLpApplication.registerBot(tgConfig.token, consumer)
                    info { "#bridgeBotFlow loaded!" }
                    send(tgLpApplication)
                } catch (e: TelegramApiErrorResponseException) {
                    info { "#bridgeBotFlow could not load event. Error ${e.message}" }
                }
                awaitClose {
                    info { "#bridgeBotFlow closing TelegramBotsLongPollingApplication..." }
                    tgLpApplication.unregisterBot(tgConfig.token)
                    tgLpApplication.stop()
                    tgLpApplication.close()
                }
            }
        }
        .flatMapLatest { tgLpApplicationFlow -> tgLpApplicationFlow }
        .shareIn(coreModule.ioScope, SharingStarted.Eagerly, 1)

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            telegramMessageController.cancel()
        }
    )
}
