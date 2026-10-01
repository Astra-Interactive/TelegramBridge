package ru.astrainteractive.messagebridge.messenger.discord.channel

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.connection.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.connection.awaitJda
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordFailureError
import kotlin.time.Duration.Companion.seconds

/** The bridge channel of config.yml and the webhook that posts into it as players. */
internal class DiscordChannelProvider(
    private val connection: StateFlow<DiscordConnection>,
    private val webhookClients: Flow<Result<WebhookClient>?>,
    private val configFlow: StateFlow<PluginConfiguration>,
) {
    private val config: PluginConfiguration
        get() = configFlow.value

    /**
     * Waits for the bot while it connects, so the messages sent on startup are not lost.
     *
     * @return `null` when the bot is turned off or could not connect
     */
    suspend fun jda(): JDA? = connection.awaitJda(CONNECTION_WAIT)

    /** @return failure with [DiscordFailureError] when the channel is not set or the bot cannot see it */
    fun textChannel(jda: JDA): Result<TextChannel> {
        val channelId = config.jdaConfig.channelId
        if (channelId.isBlank()) return Result.failure(DiscordFailureError(DiscordFailure.ChannelNotSet))
        val channel = jda.findTextChannel(channelId)
            ?: return Result.failure(DiscordFailureError(DiscordFailure.ChannelNotFound(channelId)))
        return Result.success(channel)
    }

    suspend fun webhookClient(): Result<WebhookClient> {
        val notReady = DiscordFailure.Unknown("the webhook is not ready in $CONNECTION_WAIT")
        return withTimeoutOrNull(CONNECTION_WAIT) { webhookClients.filterNotNull().first() }
            ?: Result.failure(DiscordFailureError(notReady))
    }

    private companion object {
        val CONNECTION_WAIT = 30.seconds
    }
}
