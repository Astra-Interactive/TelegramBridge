package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.model

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration

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
