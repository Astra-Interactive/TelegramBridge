package ru.astrainteractive.messagebridge.core.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PluginConfiguration(
    val jdaConfig: JdaConfig = JdaConfig(),
    val tgConfig: TelegramConfig = TelegramConfig(),
    val displayJoinMessage: Boolean = true,
    val displayLeaveMessage: Boolean = true,
    val displayDeathMessage: Boolean = true,
    val link: Link? = null
) {
    @Serializable
    data class Link(
        val linkDiscordRole: String,
        val linkLuckPermsRole: String
    )

    @Serializable
    data class Proxy(
        val host: String,
        val port: Int,
        val username: String,
        val password: String
    )

    @Serializable
    data class JdaConfig(
        val token: String = "",
        val activity: String = "",
        val channelId: String = "",
        val replyPreviewLength: Int = 60,
        val relayedMessageCacheSize: Int = 1000,
        val proxy: Proxy? = null
    )

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
        @SerialName("reply_preview_length")
        val replyPreviewLength: Int = 160,
        @SerialName("relayed_message_cache_size")
        val relayedMessageCacheSize: Int = 1000,
        @SerialName("display_name_regex")
        val displayNameRegex: String = ".*",
        @SerialName("proxy")
        val proxy: Proxy? = null
    )
}
