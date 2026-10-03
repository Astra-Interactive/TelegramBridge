package ru.astrainteractive.messagebridge.messenger.discord.model

import net.dv8tion.jda.api.JDA

sealed interface DiscordConnection {
    data object Disabled : DiscordConnection

    data object Connecting : DiscordConnection

    data class Connected(val jda: JDA) : DiscordConnection

    data class Failed(val failure: DiscordFailure) : DiscordConnection
}
