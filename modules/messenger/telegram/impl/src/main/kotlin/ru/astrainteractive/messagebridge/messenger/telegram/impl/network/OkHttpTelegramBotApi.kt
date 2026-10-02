package ru.astrainteractive.messagebridge.messenger.telegram.impl.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.future.await
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramRequestResult
import ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping.TelegramFailureMapper
import java.io.Serializable

internal class OkHttpTelegramBotApi(
    private val telegramClients: Flow<OkHttpTelegramClient?>,
    private val failureMapper: TelegramFailureMapper,
) : TelegramBotApi {
    override suspend fun <T : Serializable> execute(method: BotApiMethod<T>): TelegramRequestResult<T> {
        val client = telegramClients.firstOrNull() ?: return TelegramRequestResult.NotConnected
        return runCatching { client.executeAsync(method).await() }
            .propagateCancellationException()
            .fold(
                onSuccess = { value -> TelegramRequestResult.Success(value) },
                onFailure = { t -> TelegramRequestResult.Failed(failureMapper.map(t)) }
            )
    }
}
