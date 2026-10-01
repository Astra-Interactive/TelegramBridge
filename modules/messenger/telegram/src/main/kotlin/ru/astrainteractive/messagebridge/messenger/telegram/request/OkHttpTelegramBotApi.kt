package ru.astrainteractive.messagebridge.messenger.telegram.request

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.future.await
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailureMapper
import java.io.Serializable
import kotlin.coroutines.cancellation.CancellationException

/**
 * @param telegramClients the client of the current connection; `null` right away when the settings cannot connect,
 * e.g. the token is empty, so a request does not wait for a bot that is not coming
 */
internal class OkHttpTelegramBotApi(
    private val telegramClients: Flow<OkHttpTelegramClient?>,
    private val failureMapper: TelegramFailureMapper,
) : TelegramBotApi {
    override suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T> {
        val client = telegramClients.firstOrNull() ?: return TelegramRequestResult.NotConnected
        return runCatching { client.executeAsync(method).await() }
            .onFailure { throwable -> if (throwable is CancellationException) throw throwable }
            .fold(
                onSuccess = { value -> TelegramRequestResult.Success(value) },
                onFailure = { throwable -> TelegramRequestResult.Failed(failureMapper.map(throwable)) }
            )
    }
}
