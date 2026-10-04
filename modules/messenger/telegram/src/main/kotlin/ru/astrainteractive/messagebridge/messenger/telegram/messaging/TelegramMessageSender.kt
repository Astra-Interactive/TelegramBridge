package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.io.Serializable

internal class TelegramMessageSender(
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
) : Logger by JUtiltLogger("MessageBridge-TelegramMessageSender").withoutParentHandlers() {

    private suspend fun <T : Serializable> execute(method: BotApiMethod<T>): Result<T> {
        return runCatching { telegramClientFlow.first().executeAsync(method).await() }
            .propagateCancellationException()
    }

    suspend fun send(chatId: String, text: String, replyToMessageId: Int? = null): Message? {
        val sendMessage = SendMessage(chatId, text).apply { this.replyToMessageId = replyToMessageId }
        return execute(sendMessage)
            .onFailure { t -> error { "#send could not send a message to chat $chatId: $t" } }
            .getOrNull()
    }

    suspend fun delete(chatId: String, messageId: Int) {
        execute(DeleteMessage(chatId, messageId))
            .onFailure { t -> error { "#delete could not delete message $messageId: $t" } }
    }
}
