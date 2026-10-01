package ru.astrainteractive.messagebridge.messenger.telegram.request.fake

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.messenger.telegram.request.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.model.TelegramRequestResult
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList

internal class FakeTelegramBotApi : TelegramBotApi {
    val requests: MutableList<BotApiMethod<*>> = CopyOnWriteArrayList()

    @Volatile
    var answer: suspend (BotApiMethod<*>) -> TelegramRequestResult<*> = { method -> delivered(method) }

    val sentMessages: List<SendMessage>
        get() = requests.filterIsInstance<SendMessage>()

    val deletedMessages: List<DeleteMessage>
        get() = requests.filterIsInstance<DeleteMessage>()

    override suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T> {
        requests += method
        @Suppress("UNCHECKED_CAST")
        return answer(method) as TelegramRequestResult<T>
    }

    companion object {
        const val DELIVERED_MESSAGE_ID = 100

        fun delivered(method: BotApiMethod<*>): TelegramRequestResult<*> = when (method) {
            is SendMessage -> TelegramRequestResult.Success(
                Message().apply {
                    messageId = DELIVERED_MESSAGE_ID
                    chat = Chat(method.chatId.toLong(), "supergroup")
                }
            )
            is DeleteMessage -> TelegramRequestResult.Success(true)
            else -> error("The test did not tell what Telegram answers to ${method.method}")
        }
    }
}
