package ru.astrainteractive.messagebridge.messenger.telegram.model

import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.TelegramUrl
import ru.astrainteractive.messagebridge.core.PluginConfiguration

/** The settings a bot connection depends on; the chat and the limits apply without reconnecting. */
internal data class TelegramConnectionSettings(
    val token: String,
    val proxy: PluginConfiguration.Proxy?,
    val apiUrl: String
) {
    companion object {
        fun of(tgConfig: PluginConfiguration.TelegramConfig) = TelegramConnectionSettings(
            token = tgConfig.token.trim(),
            proxy = tgConfig.proxy,
            apiUrl = tgConfig.apiUrl.trim()
        )
    }
}

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
