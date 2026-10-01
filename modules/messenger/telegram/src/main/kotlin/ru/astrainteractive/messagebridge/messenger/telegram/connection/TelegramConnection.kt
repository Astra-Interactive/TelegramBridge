package ru.astrainteractive.messagebridge.messenger.telegram.connection

import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.TelegramUrl
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure

internal sealed interface TelegramConnection {
    /** The token is empty. */
    data object Disabled : TelegramConnection

    /** The settings cannot work, whatever the network does. */
    data class Invalid(val failure: TelegramFailure) : TelegramConnection

    class Ready(
        val token: String,
        val url: TelegramUrl,
        val okHttpClient: OkHttpClient,
        val telegramClient: OkHttpTelegramClient
    ) : TelegramConnection {
        fun close() {
            okHttpClient.dispatcher.cancelAll()
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            okHttpClient.cache?.close()
        }
    }
}
