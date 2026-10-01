package ru.astrainteractive.messagebridge.messenger.telegram.request.internal

import kotlinx.coroutines.delay
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.telegram.request.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.model.TelegramRequestResult
import java.io.Serializable
import kotlin.time.Duration

internal class TelegramMessageSender(
    private val botApi: TelegramBotApi,
    private val maxRetries: Int,
    private val retryDelay: Duration,
    logger: Logger,
) : Logger by logger {
    private fun TelegramRequestResult<*>.isTransientFailure(): Boolean {
        return this is TelegramRequestResult.Failed && failure.isTransient
    }

    private suspend fun <T : Serializable> executeWithRetry(method: BotApiMethod<T>): TelegramRequestResult<T> {
        var result = botApi.execute(method)
        repeat(maxRetries) { _ ->
            if (!result.isTransientFailure()) return result
            warn { "#executeWithRetry ${method.method} failed, retrying in $retryDelay: $result" }
            delay(retryDelay)
            result = botApi.execute(method)
        }
        return result
    }

    private suspend fun <T : Serializable> executeLogged(method: BotApiMethod<T>, description: () -> String) {
        when (val result = executeWithRetry(method)) {
            is TelegramRequestResult.Success -> Unit
            TelegramRequestResult.NotConnected -> verbose { "#executeLogged not connected, ${description()}" }
            is TelegramRequestResult.Failed -> error { "#executeLogged ${description()}: ${result.failure}" }
        }
    }

    suspend fun send(chatId: String, text: String, replyToMessageId: Int?) {
        val sendMessage = SendMessage(chatId, text).apply { this.replyToMessageId = replyToMessageId }
        executeLogged(sendMessage) { "could not send a message to chat $chatId" }
    }

    suspend fun delete(chatId: String, messageId: Int) {
        executeLogged(DeleteMessage(chatId, messageId)) { "could not delete message $messageId in chat $chatId" }
    }
}
