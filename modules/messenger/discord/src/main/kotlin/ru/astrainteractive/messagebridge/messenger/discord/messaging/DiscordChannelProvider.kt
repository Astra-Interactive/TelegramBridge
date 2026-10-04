package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DiscordChannelProvider(
    jdaConfigFlow: Flow<PluginConfiguration.JdaConfig>,
    private val connect: (PluginConfiguration.JdaConfig) -> Flow<DiscordChannel>,
    scope: CoroutineScope,
) : Logger by JUtiltLogger("MessageBridge-DiscordChannelProvider").withoutParentHandlers() {
    val channel: SharedFlow<DiscordChannel> = jdaConfigFlow
        .distinctUntilChanged()
        .flatMapLatest { config -> channelOf(config) }
        .shareIn(scope, SharingStarted.Eagerly, 1)

    private fun retryDelayAfter(failures: Int): Duration {
        val doublings = failures.coerceAtMost(MAX_DOUBLINGS)
        return (FIRST_RETRY_DELAY * (1 shl doublings)).coerceAtMost(MAX_RETRY_DELAY)
    }

    private fun channelOf(config: PluginConfiguration.JdaConfig): Flow<DiscordChannel> {
        if (config.token.isBlank() || config.channelId.isBlank()) {
            return flowOf(DiscordChannel.Disabled)
        }
        var failures = 0
        return connect.invoke(config)
            .onEach { channel -> if (channel is DiscordChannel.Ready) failures = 0 }
            .onStart { emit(DiscordChannel.Connecting) }
            .retryWhen { t, _ ->
                val retryDelay = retryDelayAfter(failures)
                failures += 1
                error { "#channel could not connect to Discord, next try in $retryDelay: ${t.message}" }
                emit(DiscordChannel.Failed)
                delay(retryDelay)
                t !is CancellationException
            }
    }

    private companion object {
        val FIRST_RETRY_DELAY = 5.seconds
        val MAX_RETRY_DELAY = 10.minutes
        const val MAX_DOUBLINGS = 7
    }
}
