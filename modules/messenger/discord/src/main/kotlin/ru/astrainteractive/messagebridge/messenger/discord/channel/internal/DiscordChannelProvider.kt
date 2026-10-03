package ru.astrainteractive.messagebridge.messenger.discord.channel.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.channel.model.DiscordChannel
import ru.astrainteractive.messagebridge.messenger.discord.channel.network.WebHookClientFactory
import ru.astrainteractive.messagebridge.messenger.discord.failure.internal.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.mapping.DiscordFailureMapper
import ru.astrainteractive.messagebridge.messenger.discord.failure.model.DiscordFailureError
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.util.findTextChannel
import kotlin.time.Duration.Companion.seconds

internal class DiscordChannelProvider(
    private val webhookClientFactory: WebHookClientFactory,
    private val failureMapper: DiscordFailureMapper,
    private val deliveryError: DiscordDeliveryError,
    connection: StateFlow<DiscordConnection>,
    private val configFlow: StateFlow<PluginConfiguration>,
    scope: CoroutineScope,
) {
    val channel: SharedFlow<DiscordChannel> = combine(
        flow = connection,
        flow2 = configFlow.map { config -> config.jdaConfig.channelId }.distinctUntilChanged(),
        transform = ::channelOf
    )
        .flatMapLatest { channel -> channel }
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    private suspend fun failed(t: Throwable): DiscordChannel.Failed {
        deliveryError.report(t)
        return DiscordChannel.Failed(failureMapper.map(t, configFlow.value.jdaConfig))
    }

    private suspend fun resolve(jda: JDA, channelId: String): Result<DiscordChannel.Ready> {
        val textChannel = jda.findTextChannel(channelId)
            ?: return Result.failure(DiscordFailureError(DiscordFailure.ChannelNotFound(channelId)))
        return webhookClientFactory.create(textChannel)
            .map { webhookClient -> DiscordChannel.Ready(textChannel, webhookClient) }
    }

    private suspend fun resolveWithRetry(
        jda: JDA,
        channelId: String,
        onFailure: suspend (Throwable) -> Unit
    ): DiscordChannel.Ready {
        while (true) {
            resolve(jda, channelId).fold(
                onSuccess = { ready -> return ready },
                onFailure = { t ->
                    onFailure.invoke(t)
                    delay(RETRY_DELAY)
                }
            )
        }
    }

    private fun bind(jda: JDA, channelId: String): Flow<DiscordChannel> = channelFlow {
        if (channelId.isBlank()) {
            send(DiscordChannel.Failed(DiscordFailure.ChannelNotSet))
            return@channelFlow
        }
        send(DiscordChannel.Connecting)
        val ready = resolveWithRetry(jda, channelId) { t -> send(failed(t)) }
        deliveryError.clear()
        send(ready)
        try {
            awaitCancellation()
        } finally {
            ready.webhookClient.close()
        }
    }

    private fun channelOf(connection: DiscordConnection, channelId: String): Flow<DiscordChannel> = when (connection) {
        DiscordConnection.Disabled -> flowOf(DiscordChannel.Disabled)
        DiscordConnection.Connecting -> flowOf(DiscordChannel.Connecting)
        is DiscordConnection.Failed -> flowOf(DiscordChannel.Failed(connection.failure))
        is DiscordConnection.Connected -> bind(connection.jda, channelId)
    }

    private companion object {
        val RETRY_DELAY = 30.seconds
    }
}
