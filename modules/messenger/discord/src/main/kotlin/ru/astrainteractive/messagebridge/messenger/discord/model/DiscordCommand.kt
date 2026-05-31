package ru.astrainteractive.messagebridge.messenger.discord.model

internal sealed interface DiscordCommand {
    data object Vanilla : DiscordCommand

    data class Link(val code: Int) : DiscordCommand
}
