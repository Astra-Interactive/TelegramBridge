package ru.astrainteractive.messagebridge.messenger.telegram.request.api

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import ru.astrainteractive.messagebridge.messenger.telegram.request.model.TelegramRequestResult
import java.io.Serializable

internal interface TelegramBotApi {
    suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T>
}
