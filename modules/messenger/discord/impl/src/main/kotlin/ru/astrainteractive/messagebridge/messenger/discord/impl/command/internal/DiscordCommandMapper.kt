package ru.astrainteractive.messagebridge.messenger.discord.impl.command.internal

import ru.astrainteractive.messagebridge.messenger.discord.impl.command.model.DiscordCommand

internal class DiscordCommandMapper {

    private fun String.parseLinkCode(): Int = replace("$LINK ", "").toIntOrNull() ?: INVALID_CODE

    fun map(contentRaw: String): DiscordCommand? = when {
        contentRaw == VANILLA -> DiscordCommand.Vanilla
        contentRaw.startsWith(LINK) -> DiscordCommand.Link(contentRaw.parseLinkCode())
        else -> null
    }

    private companion object {
        const val VANILLA = "!vanilla"
        const val LINK = "/link"
        const val INVALID_CODE = -1
    }
}
