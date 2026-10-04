package ru.astrainteractive.messagebridge.messenger.discord.di

import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.CloseCode
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.Credentials
import okhttp3.OkHttpClient
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.DiscordCommandHandler
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordCommandMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordReplyMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordBEventConsumer
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordChannelProvider
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMemberResolver
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordMessageSender
import ru.astrainteractive.messagebridge.messenger.discord.messaging.DiscordTopicUpdater
import ru.astrainteractive.messagebridge.messenger.discord.model.DisallowedIntentsError
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.util.fallbackOnDisallowedIntents
import ru.astrainteractive.messagebridge.messenger.discord.util.flowEvent
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class JdaMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
    messageInterceptors: List<MessageInterceptor<MessageReceivedEvent>>,
    authorResolver: DiscordAuthorResolver,
    private val memberLeaveListeners: List<DiscordMemberLeaveListener>
) : Logger by JUtiltLogger("MessageBridge-JdaMessengerModule").withoutParentHandlers() {
    private val ioScope = coreModule.ioScope

    private val okHttpClientFlow = coreModule.configKrate.cachedStateFlow
        .map { pluginConfiguration -> pluginConfiguration.jdaConfig.proxy }
        .distinctUntilChanged()
        .flatMapLatest { proxy ->
            callbackFlow {
                val okHttpClient = if (proxy == null) {
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
                send(okHttpClient)

                awaitClose {
                    okHttpClient.dispatcher.executorService.shutdown()
                    okHttpClient.connectionPool.evictAll()
                    okHttpClient.cache?.close()
                }
            }
        }
        .shareIn(coreModule.ioScope, SharingStarted.Lazily, 1)

    private val relevanceMapper = DiscordMessageRelevanceMapper(
        configKrate = coreModule.configKrate,
    )

    private val commandMapper = DiscordCommandMapper()

    private val messageSender = DiscordMessageSender()

    private val commandHandler = DiscordCommandHandler(
        messageSender = messageSender,
        platformServer = coreModule.platformServer,
        translationKrate = coreModule.translationKrate,
    )

    private val messageEventListener = MessageEventListener(
        relevanceMapper = relevanceMapper,
        commandMapper = commandMapper,
        commandHandler = commandHandler,
        replyMapper = DiscordReplyMapper(),
        messageSender = messageSender,
        messageInterceptors = messageInterceptors,
        bEventConsumer = bEventChannel,
    )

    private val channelProvider = DiscordChannelProvider(
        jdaConfigFlow = coreModule.configKrate.cachedStateFlow
            .map { pluginConfiguration -> pluginConfiguration.jdaConfig },
        connect = ::connect,
        scope = coreModule.ioScope,
    )

    private val discordMessageController = DiscordBEventConsumer(
        discordChannel = channelProvider.channel,
        topicUpdater = DiscordTopicUpdater(coreModule.platformServer),
        embedMapper = DiscordEmbedMapper(),
        memberResolver = DiscordMemberResolver(authorResolver),
        webhookMessageMapper = DiscordWebhookMessageMapper(),
        bEventReceiver = bEventChannel,
    )

    val bEventConsumer: BEventConsumer = discordMessageController

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            discordMessageController.cancel()
            messageEventListener.cancel()
        }
    )

    private suspend fun notifyMemberLeave(discordUserId: Long) {
        memberLeaveListeners.forEach { listener -> listener.onMemberLeave(discordUserId) }
    }

    private suspend fun sessionFailure(t: Throwable, shutdownCode: Deferred<CloseCode?>): Throwable {
        val closeCode = withTimeoutOrNull(SHUTDOWN_EVENT_TIMEOUT) { shutdownCode.await() }
        if (closeCode == CloseCode.DISALLOWED_INTENTS) return DisallowedIntentsError(t)
        return t
    }

    private fun jdaSession(
        okHttpClient: OkHttpClient,
        config: PluginConfiguration.JdaConfig,
        intents: List<GatewayIntent>
    ): Flow<JDA> = callbackFlow {
        val builder = JDABuilder.createLight(config.token).apply {
            enableIntents(intents)
            setActivity(Activity.playing(config.activity))
            setMaxReconnectDelay(MAX_RECONNECT_DELAY.inWholeSeconds.toInt())
            config.proxy?.let { proxy ->
                setWebsocketFactory(
                    WebSocketFactory()
                        .setVerifyHostname(false)
                        .also { webSocketFactory ->
                            webSocketFactory.proxySettings.setHost(proxy.host)
                            webSocketFactory.proxySettings.setPort(proxy.port)
                            webSocketFactory.proxySettings.setCredentials(proxy.username, proxy.password)
                        }
                )
                setHttpClient(okHttpClient)
            }
        }

        val jda = builder.build()
        jda.flowEvent<MessageReceivedEvent>()
            .onEach(messageEventListener::onMessageReceived)
            .launchIn(this)
        jda.flowEvent<GuildMemberRemoveEvent>()
            .map { event -> event.user }
            .map { user -> user.idLong }
            .onEach { discordUserId -> ioScope.launch { notifyMemberLeave(discordUserId) } }
            .launchIn(this)
        val shutdownCode = CompletableDeferred<CloseCode?>()
        jda.flowEvent<ShutdownEvent>()
            .onEach { event -> shutdownCode.complete(event.closeCode) }
            .launchIn(this)
        launch {
            runCatching { runInterruptible { jda.awaitReady() } }
                .propagateCancellationException()
                .fold(
                    onSuccess = { readyJda -> send(readyJda) },
                    onFailure = { t -> close(sessionFailure(t, shutdownCode)) }
                )
        }

        awaitClose {
            jda.shutdownNow()
            if (!jda.awaitShutdown(SHUTDOWN_TIMEOUT.toJavaDuration())) {
                warn { "#jdaSession JDA did not shut down within $SHUTDOWN_TIMEOUT" }
            }
        }
    }

    private fun channelSession(jda: JDA, channelId: String): Flow<DiscordChannel> = callbackFlow<DiscordChannel> {
        val textChannel = jda.getTextChannelById(channelId) ?: error("Could not find channel $channelId")
        val webhookClient = WebHookClientFactory(jda).create(channelId).first()
        send(DiscordChannel.Ready(textChannel = textChannel, webhookClient = webhookClient))
        awaitClose {
            webhookClient.close()
        }
    }.retryWhen { t, _ ->
        error { "#channelSession could not open channel $channelId: ${t.message}" }
        emit(DiscordChannel.Failed)
        delay(RETRY_DELAY)
        t !is CancellationException
    }

    private fun connect(config: PluginConfiguration.JdaConfig): Flow<DiscordChannel> = okHttpClientFlow
        .flatMapLatest { okHttpClient ->
            jdaSession(okHttpClient, config, MESSAGE_INTENTS + GatewayIntent.GUILD_MEMBERS)
                .fallbackOnDisallowedIntents(
                    onFallback = { warn { "#connect Server Members Intent is off: member leaves are not tracked" } },
                    fallback = { jdaSession(okHttpClient, config, MESSAGE_INTENTS) }
                )
        }
        .flatMapLatest { jda -> channelSession(jda, config.channelId) }

    private companion object {
        val MAX_RECONNECT_DELAY = 32.seconds
        val RETRY_DELAY = 5.seconds
        val SHUTDOWN_TIMEOUT = 10.seconds
        val SHUTDOWN_EVENT_TIMEOUT = 5.seconds
        val MESSAGE_INTENTS = listOf(
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_MESSAGES
        )
    }
}
