@file:Suppress("KindImportDirection")

package ru.astrainteractive.messagebridge.messenger.discord.connection

import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.requests.CloseCode
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.OkHttpClient
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DisallowedIntentsError
import ru.astrainteractive.messagebridge.messenger.discord.util.fallbackOnDisallowedIntents
import ru.astrainteractive.messagebridge.messenger.discord.util.flowEvent
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class JdaSessionConnector(
    private val okHttpClientFlow: Flow<OkHttpClient>,
    private val eventManager: IEventManager
) : Logger by JUtiltLogger("MessageBridge-JdaSessionConnector") {

    private fun proxiedWebSocketFactory(proxy: PluginConfiguration.Proxy): WebSocketFactory {
        return WebSocketFactory()
            .setVerifyHostname(false)
            .also { webSocketFactory ->
                webSocketFactory.proxySettings.setHost(proxy.host)
                webSocketFactory.proxySettings.setPort(proxy.port)
                webSocketFactory.proxySettings.setCredentials(proxy.username, proxy.password)
            }
    }

    private fun jdaBuilder(
        okHttpClient: OkHttpClient,
        config: PluginConfiguration.JdaConfig,
        intents: List<GatewayIntent>
    ): JDABuilder {
        return JDABuilder.createLight(config.token).apply {
            enableIntents(intents)
            setEventManager(eventManager)
            setActivity(Activity.playing(config.activity))
            setMaxReconnectDelay(MAX_RECONNECT_DELAY.inWholeSeconds.toInt())
            setHttpClient(okHttpClient)
            config.proxy?.let { proxy ->
                setWebsocketFactory(proxiedWebSocketFactory(proxy))
            }
        }
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
    ): Flow<JDA> {
        return callbackFlow {
            val jda = jdaBuilder(
                okHttpClient = okHttpClient,
                config = config,
                intents = intents
            ).build()
            val shutdownCode = CompletableDeferred<CloseCode?>()
            jda.eventManager.flowEvent<ShutdownEvent>()
                .filter { event -> event.jda === jda }
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
    }

    private fun jdaSessionWithFallback(okHttpClient: OkHttpClient, config: PluginConfiguration.JdaConfig): Flow<JDA> {
        return jdaSession(
            okHttpClient = okHttpClient,
            config = config,
            intents = MESSAGE_INTENTS + GatewayIntent.GUILD_MEMBERS
        ).fallbackOnDisallowedIntents(
            onFallback = { warn { "#connect Server Members Intent is off: member leaves are not tracked" } },
            fallback = {
                jdaSession(
                    okHttpClient = okHttpClient,
                    config = config,
                    intents = MESSAGE_INTENTS
                )
            }
        )
    }

    fun connect(config: PluginConfiguration.JdaConfig): Flow<JDA> {
        return okHttpClientFlow
            .flatMapLatest { okHttpClient -> jdaSessionWithFallback(okHttpClient, config) }
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
