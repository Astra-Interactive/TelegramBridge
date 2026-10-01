package ru.astrainteractive.messagebridge.messenger.discord.mapping

import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordCommand

internal class DiscordCommandMapper {

    fun map(contentRaw: String): DiscordCommand? = when {
        contentRaw == VANILLA -> DiscordCommand.Vanilla
        contentRaw.startsWith(LINK) -> DiscordCommand.Link(contentRaw.parseLinkCode())
        else -> null
    }

    /** @return the code of `!bind <code>`, empty when the code is missing, `null` for any other message */
    fun mapBindCode(contentRaw: String): String? {
        val words = contentRaw.trim().split(WHITESPACE)
        if (words.first() != BIND) return null
        return words.getOrNull(1).orEmpty()
    }

    private fun String.parseLinkCode(): Int = replace("$LINK ", "").toIntOrNull() ?: INVALID_CODE

    private companion object {
        const val VANILLA = "!vanilla"
        const val LINK = "/link"
        const val BIND = "!bind"
        const val INVALID_CODE = -1
        val WHITESPACE = Regex("\\s+")
    }
}
