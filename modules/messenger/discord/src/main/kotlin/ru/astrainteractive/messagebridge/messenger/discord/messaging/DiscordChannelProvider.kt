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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
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

    private fun channelOf(config: PluginConfiguration.JdaConfig): Flow<DiscordChannel> {
        if (config.token.isBlank() || config.channelId.isBlank()) {
            return flowOf(DiscordChannel.Disabled)
        }
        return connect.invoke(config)
            .onStart { emit(DiscordChannel.Connecting) }
            .retryWhen { t, _ ->
                error { "#channel could not connect to Discord: ${t.message}" }
                emit(DiscordChannel.Failed)
                delay(RETRY_DELAY)
                t !is CancellationException
            }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
    }
}
