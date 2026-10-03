package ru.astrainteractive.messagebridge.messenger.discord.connection.network

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
import ru.astrainteractive.messagebridge.core.api.config.ProxyType
import ru.astrainteractive.messagebridge.messenger.discord.connection.event.JdaShutdownListener
import ru.astrainteractive.messagebridge.messenger.discord.connection.internal.ExponentialBackoff
import ru.astrainteractive.messagebridge.messenger.discord.connection.model.DiscordConnectionSettings
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.util.privilegedNames
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class DiscordConnector(
    private val jdaBuilderFactory: JdaBuilderFactory,
    private val failureMapper: DiscordFailureMapper,
    private val backoff: ExponentialBackoff,
) : Logger by JUtiltLogger("MessageBridge-DiscordConnector") {

    private fun OkHttpClient.release() {
        dispatcher.cancelAll()
        dispatcher.executorService.shutdown()
        connectionPool.evictAll()
        cache?.close()
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

    private suspend fun JDA.awaitReadyOrShutdown(): Boolean {
        return try {
            runInterruptible { awaitReady() }
            true
        } catch (ignored: IllegalStateException) {
            false
        }
    }

    private suspend fun holdSession(
        settings: DiscordConnectionSettings,
        okHttpClient: OkHttpClient?,
        onConnected: suspend (JDA) -> Unit
    ): DiscordFailure {
        val shutdownListener = JdaShutdownListener()
        val privilegedIntents = settings.intents.privilegedNames()
        val jda = try {
            runInterruptible {
                jdaBuilderFactory.create(settings, okHttpClient)
                    .addEventListeners(shutdownListener)
                    .build()
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Exception) {
            return failureMapper.map(t, settings.jdaConfig)
        }
        try {
            if (jda.awaitReadyOrShutdown()) {
                onConnected.invoke(jda)
                return failureMapper.mapShutdown(shutdownListener.awaitCloseCode(), privilegedIntents)
            }
            val closeCode = withTimeoutOrNull(SHUTDOWN_EVENT_TIMEOUT) { shutdownListener.awaitCloseCode() }
            return failureMapper.mapShutdown(closeCode, privilegedIntents)
        } finally {
            withContext(NonCancellable) { jda.shutdownAndWait() }
        }
    }

    fun connect(settings: DiscordConnectionSettings): Flow<DiscordConnection> = when {
        settings.jdaConfig.token.isBlank() -> flowOf(DiscordConnection.Disabled)
        settings.jdaConfig.proxy?.type == ProxyType.SOCKS5 -> {
            flowOf(DiscordConnection.Failed(DiscordFailure.SocksNotSupported))
        }

        else -> channelFlow {
            send(DiscordConnection.Connecting)
            val okHttpClient = settings.jdaConfig.proxy?.let(jdaBuilderFactory::createOkHttpClient)
            try {
                var failedAttempts = 0
                while (true) {
                    val failure = holdSession(settings, okHttpClient) { jda ->
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
        }.catch { t -> emit(DiscordConnection.Failed(failureMapper.map(t, settings.jdaConfig))) }
    }

    private companion object {
        val SHUTDOWN_EVENT_TIMEOUT = 5.seconds
        val SHUTDOWN_TIMEOUT = 10.seconds
    }
}
