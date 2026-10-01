package ru.astrainteractive.messagebridge.messenger.discord.command.internal

import ru.astrainteractive.messagebridge.messenger.discord.command.model.DiscordCommand

internal class DiscordCommandMapper {

    private fun String.parseLinkCode(): Int = replace("$LINK ", "").toIntOrNull() ?: INVALID_CODE

    fun map(contentRaw: String): DiscordCommand? = when {
        contentRaw == VANILLA -> DiscordCommand.Vanilla
        contentRaw.startsWith(LINK) -> DiscordCommand.Link(contentRaw.parseLinkCode())
        else -> null
    }

    fun mapBindCode(contentRaw: String): String? {
        val words = contentRaw.trim().split(WHITESPACE)
        if (words.first() != BIND) return null
        return words.getOrNull(1).orEmpty()
    }

    private companion object {
        const val VANILLA = "!vanilla"
        const val LINK = "/link"
        const val BIND = "!bind"
        const val INVALID_CODE = -1
        val WHITESPACE = Regex("\\s+")
    }
}
