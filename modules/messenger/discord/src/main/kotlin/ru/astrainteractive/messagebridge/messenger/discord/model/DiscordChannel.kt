package ru.astrainteractive.messagebridge.messenger.discord.model

import club.minnced.discord.webhook.WebhookClient
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

internal sealed interface DiscordChannel {
    data object Disabled : DiscordChannel

    data object Connecting : DiscordChannel

    data object Failed : DiscordChannel

    data class Ready(
        val textChannel: TextChannel,
        val webhookClient: WebhookClient
    ) : DiscordChannel
}
