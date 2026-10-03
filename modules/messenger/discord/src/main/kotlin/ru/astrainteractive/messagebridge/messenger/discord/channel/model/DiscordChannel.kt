package ru.astrainteractive.messagebridge.messenger.discord.channel.model

import club.minnced.discord.webhook.WebhookClient
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordFailure

internal sealed interface DiscordChannel {
    data object Disabled : DiscordChannel

    data object Connecting : DiscordChannel

    data class Ready(val textChannel: TextChannel, val webhookClient: WebhookClient) : DiscordChannel

    data class Failed(val failure: DiscordFailure) : DiscordChannel
}
