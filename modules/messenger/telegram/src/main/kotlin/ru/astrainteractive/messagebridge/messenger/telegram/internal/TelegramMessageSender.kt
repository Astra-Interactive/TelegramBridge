package ru.astrainteractive.messagebridge.messenger.telegram.internal

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import java.io.Serializable
import kotlin.time.Duration.Companion.seconds

internal class TelegramMessageSender(
    private val telegramClientFlow: Flow<OkHttpTelegramClient>,
) : Logger by JUtiltLogger("MessageBridge-TelegramMessageSender").withoutParentHandlers() {

    private suspend fun <T : Serializable> execute(method: BotApiMethod<T>): Result<T> {
        return runCatching { telegramClientFlow.first().executeAsync(method).await() }
            .propagateCancellationException()
    }

    private suspend fun <T : Serializable> executeAfterFloodWait(method: BotApiMethod<T>): Result<T> {
        val result = execute(method)
        val retryAfter = result.exceptionOrNull()
            ?.tryCast<TelegramApiRequestException>()
            ?.parameters
            ?.retryAfter
            ?.seconds
            ?: return result
        if (retryAfter > MAX_FLOOD_WAIT) return result
        warn { "#executeAfterFloodWait Telegram asked to wait $retryAfter before ${method.method}" }
        delay(retryAfter)
        return execute(method)
    }

    suspend fun send(chatId: String, text: String, replyToMessageId: Int? = null): Message? {
        val sendMessage = SendMessage(chatId, text).apply { this.replyToMessageId = replyToMessageId }
        return executeAfterFloodWait(sendMessage)
            .onFailure { t -> error { "#send could not send a message to chat $chatId: $t" } }
            .getOrNull()
    }

    suspend fun delete(chatId: String, messageId: Int) {
        executeAfterFloodWait(DeleteMessage(chatId, messageId))
            .onFailure { t -> error { "#delete could not delete message $messageId: $t" } }
    }

    private companion object {
        val MAX_FLOOD_WAIT = 30.seconds
    }
}
