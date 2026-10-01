package ru.astrainteractive.messagebridge.messenger.discord.impl.command.model

internal sealed interface DiscordCommand {
    data object Vanilla : DiscordCommand
}
