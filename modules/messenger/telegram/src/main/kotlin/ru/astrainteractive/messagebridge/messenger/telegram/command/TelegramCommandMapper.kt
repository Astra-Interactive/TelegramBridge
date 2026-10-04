package ru.astrainteractive.messagebridge.messenger.telegram.command

import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramCommand

internal class TelegramCommandMapper {

    fun map(text: String): TelegramCommand? = when {
        text == VANILLA -> TelegramCommand.Vanilla
        else -> null
    }

    private companion object {
        const val VANILLA = "/vanilla"
    }
}
