package ru.astrainteractive.messagebridge.messenger.discord.messaging

import club.minnced.discord.webhook.WebhookClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailureException
import ru.astrainteractive.messagebridge.messenger.discord.model.awaitJda
import ru.astrainteractive.messagebridge.messenger.discord.util.findTextChannel
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

internal class DiscordChannelProvider(
    private val connection: StateFlow<DiscordConnection>,
    private val webHookClientFlow: Flow<Result<WebhookClient>?>,
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

    fun textChannel(jda: JDA): TextChannel {
        val channelId = config.jdaConfig.channelId
        if (channelId.isBlank()) throw DiscordFailureException(DiscordFailure.ChannelNotSet)
        return jda.findTextChannel(channelId)
            ?: throw DiscordFailureException(DiscordFailure.ChannelNotFound(channelId))
    }

    suspend fun webHookClient(): WebhookClient {
        val webHookClient = withTimeoutOrNull(CONNECTION_WAIT) { webHookClientFlow.filterNotNull().first() }
            ?: throw IOException("the webhook is not ready after $CONNECTION_WAIT")
        return webHookClient.getOrThrow()
    }

    private companion object {
        val CONNECTION_WAIT = 30.seconds
    }
}
