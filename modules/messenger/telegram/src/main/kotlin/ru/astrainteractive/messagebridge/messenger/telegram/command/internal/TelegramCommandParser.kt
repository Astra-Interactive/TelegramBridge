package ru.astrainteractive.messagebridge.messenger.telegram.command.internal

import ru.astrainteractive.messagebridge.messenger.telegram.command.model.TelegramCommand

internal class TelegramCommandParser(
    private val botUserName: () -> String?,
) {
    private fun String.isForThisBot(): Boolean {
        val mention = substringAfter(MENTION, missingDelimiterValue = "")
        if (mention.isEmpty()) return true
        val userName = botUserName() ?: return true
        return mention.equals(userName, ignoreCase = true)
    }

    fun map(text: String): TelegramCommand? {
        val parts = text.trim().split(WHITESPACE, limit = 2)
        val command = parts.first()
        if (!command.isForThisBot()) return null
        val argument = parts.getOrNull(1).orEmpty().trim()
        return when (command.substringBefore(MENTION)) {
            VANILLA -> TelegramCommand.Vanilla
            LINK -> TelegramCommand.Link(argument.toIntOrNull() ?: INVALID_CODE)
            BIND -> TelegramCommand.Bind(argument)
            INFO -> TelegramCommand.ChatInfo
            else -> null
        }
    }

    private companion object {
        const val VANILLA = "/vanilla"
        const val LINK = "/link"
        const val BIND = "/bind"
        const val INFO = "/minfo"
        const val MENTION = '@'
        const val INVALID_CODE = -1
        val WHITESPACE = "\\s+".toRegex()
    }
}
