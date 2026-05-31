package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.withRetry
import java.io.Serializable

internal class TelegramMessageSender(
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
) : Logger by JUtiltLogger("MessageBridge-TelegramMessageSender").withoutParentHandlers() {

    suspend fun send(chatId: String, text: String, replyToMessageId: Int? = null) {
        val sendMessage = SendMessage(chatId, text).apply { this.replyToMessageId = replyToMessageId }
        executeWithRetry(sendMessage) { "#send could not send message to chat $chatId" }
    }

    suspend fun delete(chatId: String, messageId: Int) {
        executeWithRetry(DeleteMessage(chatId, messageId)) { "#delete could not delete message $messageId" }
    }

    private suspend fun <T : Serializable> executeWithRetry(method: BotApiMethod<T>, errorMessage: () -> String) {
        flow { emit(clientOrNull()?.execute(method)) }
            .withRetry()
            .catch { error(it) { errorMessage() } }
            .collect()
    }

    private suspend fun clientOrNull(): OkHttpTelegramClient? {
        return runCatching { telegramClientFlow.firstOrNull() }
            .onFailure { error(it) { "#clientOrNull could not resolve telegram client: ${it.message}" } }
            .getOrNull()
    }
}
