package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramCommand

/**
 * Parses raw message text into a [ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramCommand], or null when it is an ordinary chat message.
 */
internal class TelegramCommandMapper {

    fun map(text: String): TelegramCommand? = when {
        text == VANILLA -> TelegramCommand.Vanilla
        text.startsWith(LINK) -> TelegramCommand.Link(text.parseLinkCode())
        else -> null
    }

    private fun String.parseLinkCode(): Int = replace("$LINK ", "").toIntOrNull() ?: INVALID_CODE

    private companion object {
        const val VANILLA = "/vanilla"
        const val LINK = "/link"
        const val INVALID_CODE = -1
    }
}