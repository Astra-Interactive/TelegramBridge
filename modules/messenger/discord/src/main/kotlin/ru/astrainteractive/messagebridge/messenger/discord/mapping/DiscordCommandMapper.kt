package ru.astrainteractive.messagebridge.messenger.discord.mapping

import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordCommand

internal class DiscordCommandMapper {

    fun map(contentRaw: String): DiscordCommand? = when {
        contentRaw == VANILLA -> DiscordCommand.Vanilla
        contentRaw.startsWith(LINK) -> DiscordCommand.Link(contentRaw.parseLinkCode())
        else -> null
    }

    private fun String.parseLinkCode(): Int = replace("$LINK ", "").toIntOrNull() ?: INVALID_CODE

    private companion object {
        const val VANILLA = "!vanilla"
        const val LINK = "/link"
        const val INVALID_CODE = -1
    }
}
