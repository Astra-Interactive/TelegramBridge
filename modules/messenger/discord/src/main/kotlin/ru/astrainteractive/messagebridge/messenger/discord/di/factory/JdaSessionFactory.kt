package ru.astrainteractive.messagebridge.messenger.discord.di.factory

import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import ru.astrainteractive.klibs.kstorage.api.StateFlowKrate
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordMemberLeaveListener
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.messenger.discord.internal.fallbackOnDisallowedIntents
import ru.astrainteractive.messagebridge.messenger.discord.internal.flowEvent
import ru.astrainteractive.messagebridge.messenger.discord.model.DisallowedIntentsError
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class JdaSessionFactory(
    configKrate: StateFlowKrate<PluginConfiguration>,
    private val messageEventListener: MessageEventListener,
    private val memberLeaveListeners: List<DiscordMemberLeaveListener>,
    private val ioScope: CoroutineScope
) : Logger by JUtiltLogger("MessageBridge-JdaSessionFactory").withoutParentHandlers() {
    private val okHttpClientFlow = configKrate.cachedStateFlow
        .map { pluginConfiguration -> pluginConfiguration.jdaConfig.proxy }
        .distinctUntilChanged()
        .flatMapLatest { proxy -> okHttpClientSession(proxy) }
        .shareIn(ioScope, SharingStarted.Lazily, 1)

    @Suppress("MagicNumber")
    private fun jdaOkHttpClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 25 })
        .connectionPool(ConnectionPool(5, 10, TimeUnit.SECONDS))

    @Suppress("MagicNumber")
    private fun proxiedOkHttpClient(proxy: PluginConfiguration.Proxy): OkHttpClient = jdaOkHttpClientBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
        .proxyAuthenticator { route, response ->
            val builder = response.request.newBuilder()
            if (route?.socketAddress?.hostString == proxy.host) {
                val credential: String = Credentials.basic(proxy.username, proxy.password)
                builder.header("Proxy-Authorization", credential)
            }
            builder.build()
        }
        .build()

    private fun okHttpClientSession(proxy: PluginConfiguration.Proxy?): Flow<OkHttpClient> = callbackFlow {
        val okHttpClient = if (proxy == null) {
            jdaOkHttpClientBuilder().build()
        } else {
            proxiedOkHttpClient(proxy)
        }
        send(okHttpClient)

        awaitClose {
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            okHttpClient.cache?.close()
        }
    }

    private fun proxiedWebSocketFactory(proxy: PluginConfiguration.Proxy): WebSocketFactory = WebSocketFactory()
        .setVerifyHostname(false)
        .also { webSocketFactory ->
            webSocketFactory.proxySettings.setHost(proxy.host)
            webSocketFactory.proxySettings.setPort(proxy.port)
            webSocketFactory.proxySettings.setCredentials(proxy.username, proxy.password)
        }

    private fun jdaBuilder(
        okHttpClient: OkHttpClient,
        config: PluginConfiguration.JdaConfig,
        intents: List<GatewayIntent>
    ): JDABuilder = JDABuilder.createLight(config.token).apply {
        enableIntents(intents)
        setActivity(Activity.playing(config.activity))
        setMaxReconnectDelay(MAX_RECONNECT_DELAY.inWholeSeconds.toInt())
        setHttpClient(okHttpClient)
        config.proxy?.let { proxy ->
            setWebsocketFactory(proxiedWebSocketFactory(proxy))
        }
    }

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
        val jda = jdaBuilder(okHttpClient, config, intents).build()
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

    fun create(config: PluginConfiguration.JdaConfig): Flow<JDA> = okHttpClientFlow
        .flatMapLatest { okHttpClient ->
            jdaSession(okHttpClient, config, MESSAGE_INTENTS + GatewayIntent.GUILD_MEMBERS)
                .fallbackOnDisallowedIntents(
                    onFallback = { warn { "#create Server Members Intent is off: member leaves are not tracked" } },
                    fallback = { jdaSession(okHttpClient, config, MESSAGE_INTENTS) }
                )
        }

    private companion object {
        val MAX_RECONNECT_DELAY = 32.seconds
        val SHUTDOWN_TIMEOUT = 10.seconds
        val SHUTDOWN_EVENT_TIMEOUT = 5.seconds
        val MESSAGE_INTENTS = listOf(
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_MESSAGES
        )
    }
}
