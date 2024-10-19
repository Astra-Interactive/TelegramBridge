package ru.astrainteractive.messagebridge.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PluginConfiguration(
    val jdaConfig: JdaConfig = JdaConfig(),
    val tgConfig: TelegramConfig = TelegramConfig(),
    val displayJoinMessage: Boolean = true,
    val displayLeaveMessage: Boolean = true,
    val displayDeathMessage: Boolean = true,
) {
    @Serializable
    data class JdaConfig(
        val token: String = "",
        val activity: String = "",
        val channelId: String = "",
        val proxy: Proxy? = null
    ) {
        @Serializable
        data class Proxy(
            val host: String,
            val port: Int,
            val username: String,
            val password: String
        )
    }

    @Serializable
    data class TelegramConfig(
        @SerialName("token")
        val token: String = "",
        @SerialName("chat_id")
        val chatID: String = "",
        @SerialName("topic_id")
        val topicID: String = "",
        @SerialName("max_telegram_message_length")
        val maxTelegramMessageLength: Int = 90,
    )
}
