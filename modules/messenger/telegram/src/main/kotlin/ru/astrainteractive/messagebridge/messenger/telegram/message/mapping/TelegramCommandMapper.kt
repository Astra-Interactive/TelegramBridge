package ru.astrainteractive.messagebridge.messenger.telegram.message.mapping

import ru.astrainteractive.messagebridge.messenger.telegram.message.model.TelegramCommand

internal class TelegramCommandMapper {

    fun toCommand(text: String): TelegramCommand? {
        return when {
            text == VANILLA -> TelegramCommand.Vanilla
            else -> null
        }
    }

    private companion object {
        const val VANILLA = "/vanilla"
    }
}
