package ru.astrainteractive.messagebridge.messenger.telegram.request

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import java.io.Serializable

/** Requests to the Bot API through the current connection of the bot. */
internal interface TelegramBotApi {
    /** Makes one attempt; a transient failure is not repeated here. */
    suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T>
}
