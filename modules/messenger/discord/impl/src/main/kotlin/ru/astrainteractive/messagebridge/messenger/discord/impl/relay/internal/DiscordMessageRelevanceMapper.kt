package ru.astrainteractive.messagebridge.messenger.discord.impl.relay.internal

import kotlinx.coroutines.flow.StateFlow
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.impl.relay.model.DiscordMessageRelevance

internal class DiscordMessageRelevanceMapper(
    private val configFlow: StateFlow<PluginConfiguration>,
) {
    private val config: PluginConfiguration
        get() = configFlow.value

    fun map(event: MessageReceivedEvent): DiscordMessageRelevance = when {
        event.isWebhookMessage -> DiscordMessageRelevance.WebhookMessage
        event.author.isBot -> DiscordMessageRelevance.BotAuthor
        event.channelType == ChannelType.PRIVATE -> DiscordMessageRelevance.PrivateMessage
        event.message.channelId != config.jdaConfig.channelId -> DiscordMessageRelevance.WrongChannel
        else -> DiscordMessageRelevance.Relevant
    }
}
