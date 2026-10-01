package ru.astrainteractive.messagebridge.messenger.telegram.connection

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
