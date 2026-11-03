package ru.astrainteractive.messagebridge.messenger.discord.message.mapping

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.model.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.discord.message.model.DiscordMessageRelevance

internal class DiscordMessageRelevanceMapper(
    configKrate: CachedKrate<PluginConfiguration>,
) {
    private val config by configKrate

    fun toRelevance(event: MessageReceivedEvent): DiscordMessageRelevance {
        return when {
            event.isWebhookMessage -> DiscordMessageRelevance.WebhookMessage
            event.author.isBot -> DiscordMessageRelevance.BotAuthor
            event.channelType == ChannelType.PRIVATE -> DiscordMessageRelevance.PrivateMessage
            event.message.channelId != config.jdaConfig.channelId -> DiscordMessageRelevance.WrongChannel
            else -> DiscordMessageRelevance.Relevant
        }
    }
}
