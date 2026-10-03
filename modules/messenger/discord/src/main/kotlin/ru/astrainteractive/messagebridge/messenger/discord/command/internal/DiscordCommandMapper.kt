package ru.astrainteractive.messagebridge.messenger.discord.command.internal

import ru.astrainteractive.messagebridge.messenger.discord.command.model.DiscordCommand

internal class DiscordCommandMapper {
    fun map(contentRaw: String): DiscordCommand? = when (contentRaw) {
        VANILLA -> DiscordCommand.Vanilla
        else -> null
    }

    private companion object {
        const val VANILLA = "!vanilla"
    }
}
