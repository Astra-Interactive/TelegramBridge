package ru.astrainteractive.messagebridge.messenger.telegram.api.api

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramRequestResult
import java.io.Serializable

interface TelegramBotApi {
    suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T>
}
