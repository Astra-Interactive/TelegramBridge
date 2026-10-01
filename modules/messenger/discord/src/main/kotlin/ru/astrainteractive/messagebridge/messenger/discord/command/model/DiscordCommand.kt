package ru.astrainteractive.messagebridge.messenger.discord.command.model

internal sealed interface DiscordCommand {
    data object Vanilla : DiscordCommand

    data class Link(val code: Int) : DiscordCommand
}
