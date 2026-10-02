package ru.astrainteractive.messagebridge.onboarding.telegram.command

import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramBotCommand
import ru.astrainteractive.messagebridge.onboarding.telegram.model.TelegramSetupCommand

internal class TelegramSetupCommandParser(
    private val botUserName: () -> String?,
) {
    fun map(text: String): TelegramSetupCommand? {
        val command = TelegramBotCommand.parse(text, botUserName.invoke()) ?: return null
        return when (command.name) {
            BIND -> TelegramSetupCommand.Bind(command.argument)
            CHAT_INFO -> TelegramSetupCommand.ChatInfo
            else -> null
        }
    }

    private companion object {
        const val BIND = "/bind"
        const val CHAT_INFO = "/minfo"
    }
}
