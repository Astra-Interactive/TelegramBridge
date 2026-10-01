package ru.astrainteractive.messagebridge.messenger.telegram.impl.request.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.future.await
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult
import ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping.TelegramFailureMapper
import java.io.Serializable
import kotlin.coroutines.cancellation.CancellationException

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
