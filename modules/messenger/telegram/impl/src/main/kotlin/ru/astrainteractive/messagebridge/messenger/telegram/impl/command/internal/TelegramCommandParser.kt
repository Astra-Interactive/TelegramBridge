package ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal

import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramBotCommand
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.model.TelegramCommand

internal class TelegramCommandParser(
    private val botUserName: suspend () -> String?,
) {
    suspend fun map(text: String): TelegramCommand? {
        val command = TelegramBotCommand.parse(text, botUserName()) ?: return null
        return when (command.name) {
            VANILLA -> TelegramCommand.Vanilla
            else -> null
        }
    }

    private companion object {
        const val VANILLA = "/vanilla"
    }
}
