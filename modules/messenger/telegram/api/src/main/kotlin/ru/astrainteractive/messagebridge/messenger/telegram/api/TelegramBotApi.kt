package ru.astrainteractive.messagebridge.messenger.telegram.api

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import java.io.Serializable

interface TelegramBotApi {
    suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T>
}
