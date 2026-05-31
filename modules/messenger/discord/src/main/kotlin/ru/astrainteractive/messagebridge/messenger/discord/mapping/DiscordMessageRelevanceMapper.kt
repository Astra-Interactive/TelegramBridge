package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordMessageRelevance

internal class DiscordMessageRelevanceMapper(
    configKrate: CachedKrate<PluginConfiguration>,
) {
    private val config by configKrate

    fun map(event: MessageReceivedEvent): DiscordMessageRelevance = when {
        event.isWebhookMessage -> DiscordMessageRelevance.WebhookMessage
        event.author.isBot -> DiscordMessageRelevance.BotAuthor
        event.channelType == ChannelType.PRIVATE -> DiscordMessageRelevance.PrivateMessage
        event.message.channelId != config.jdaConfig.channelId -> DiscordMessageRelevance.WrongChannel
        else -> DiscordMessageRelevance.Relevant
    }
}
