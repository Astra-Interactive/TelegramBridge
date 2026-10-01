package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import okhttp3.OkHttpClient
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.di.factory.JdaBuilderFactory
import ru.astrainteractive.messagebridge.messenger.discord.event.core.JdaShutdownListener
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.model.JdaShutdownException
import ru.astrainteractive.messagebridge.messenger.discord.util.ExponentialBackoff
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class DiscordConnector(
    private val jdaBuilderFactory: JdaBuilderFactory,
    private val failureMapper: DiscordFailureMapper,
    private val backoff: ExponentialBackoff = ExponentialBackoff(),
) : Logger by JUtiltLogger("MessageBridge-DiscordConnector") {

    /**
     * Keeps the bot connected with [config] until the flow is cancelled. A failure the settings have to fix, like a
     * wrong token, is not retried; network errors are retried with a growing delay.
     */
    fun connect(config: PluginConfiguration.JdaConfig): Flow<DiscordConnection> = when {
        config.token.isBlank() -> flowOf(DiscordConnection.Disabled)
        config.proxy?.type == PluginConfiguration.Proxy.Type.SOCKS5 -> {
            flowOf(DiscordConnection.Failed(DiscordFailure.SocksNotSupported))
        }

        else -> channelFlow {
            send(DiscordConnection.Connecting)
            val okHttpClient = config.proxy?.let(jdaBuilderFactory::createOkHttpClient)
            try {
                var failedAttempts = 0
                while (true) {
                    val failure = holdSession(config, okHttpClient) { jda ->
                        failedAttempts = 0
                        send(DiscordConnection.Connected(jda))
                    }
                    send(DiscordConnection.Failed(failure))
                    if (!failure.isRetryable) break
                    val retryDelay = backoff.delayFor(failedAttempts++)
                    verbose { "#connect reconnecting in $retryDelay after $failure" }
                    delay(retryDelay)
                }
            } finally {
                okHttpClient?.release()
            }
        }.catch { throwable -> emit(DiscordConnection.Failed(failureMapper.map(throwable, config))) }
    }

    /**
     * Connects the bot and holds the session until JDA stops by itself.
     *
     * @return why the bot could not connect or why it stopped
     */
    private suspend fun holdSession(
        config: PluginConfiguration.JdaConfig,
        okHttpClient: OkHttpClient?,
        onConnected: suspend (JDA) -> Unit
    ): DiscordFailure {
        val shutdownListener = JdaShutdownListener()
        val jda = try {
            runInterruptible {
                jdaBuilderFactory.create(config, okHttpClient)
                    .addEventListeners(shutdownListener)
                    .build()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return failureMapper.map(e, config)
        }
        try {
            if (jda.awaitReadyOrShutdown()) {
                onConnected(jda)
                return failureMapper.map(JdaShutdownException(shutdownListener.awaitCloseCode()), config)
            }
            // The shutdown event comes right after awaitReady gives up
            val closeCode = withTimeoutOrNull(SHUTDOWN_EVENT_TIMEOUT) { shutdownListener.awaitCloseCode() }
            return failureMapper.map(JdaShutdownException(closeCode), config)
        } finally {
            withContext(NonCancellable) { jda.shutdownAndWait() }
        }
    }

    /** @return `false` when JDA was shut down before it got ready, e.g. by close code 4014 */
    private suspend fun JDA.awaitReadyOrShutdown(): Boolean {
        return try {
            runInterruptible { awaitReady() }
            true
        } catch (ignored: IllegalStateException) {
            false
        }
    }

    private fun JDA.shutdownAndWait() {
        shutdownNow()
        try {
            awaitShutdown(SHUTDOWN_TIMEOUT.toJavaDuration())
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        registeredListeners.forEach(::removeEventListener)
    }

    private fun OkHttpClient.release() {
        dispatcher.cancelAll()
        dispatcher.executorService.shutdown()
        connectionPool.evictAll()
        cache?.close()
    }

    private companion object {
        val SHUTDOWN_EVENT_TIMEOUT = 5.seconds
        val SHUTDOWN_TIMEOUT = 10.seconds
    }
}
