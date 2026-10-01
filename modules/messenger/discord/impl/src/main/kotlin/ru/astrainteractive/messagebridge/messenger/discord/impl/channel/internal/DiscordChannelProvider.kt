package ru.astrainteractive.messagebridge.messenger.discord.impl.channel.internal

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.api.model.awaitJda
import ru.astrainteractive.messagebridge.messenger.discord.api.util.findTextChannel
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.model.DiscordFailureError
import kotlin.time.Duration.Companion.seconds

internal class DiscordChannelProvider(
    private val connection: StateFlow<DiscordConnection>,
    private val webhookClients: Flow<Result<WebhookClient>?>,
    private val configFlow: StateFlow<PluginConfiguration>,
) {
    private val config: PluginConfiguration
        get() = configFlow.value

    suspend fun jda(): JDA? = connection.awaitJda(CONNECTION_WAIT)

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
