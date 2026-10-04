package ru.astrainteractive.messagebridge.messenger.discord.command

import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordCommand

internal class DiscordCommandMapper {

    fun map(contentRaw: String): DiscordCommand? = when {
        contentRaw == VANILLA -> DiscordCommand.Vanilla
        else -> null
    }

    private companion object {
        const val VANILLA = "!vanilla"
    }
}
