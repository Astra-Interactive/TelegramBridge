package ru.astrainteractive.messagebridge.messenger.discord.message.model

internal sealed interface DiscordCommand {
    data object Vanilla : DiscordCommand
}
