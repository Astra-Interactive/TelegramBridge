package ru.astrainteractive.messagebridge.messenger.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageRef {
    @Serializable
    @SerialName("TelegramMessageRef")
    data class Telegram(
        val chatId: Long,
        val messageId: Int
    ) : MessageRef

    @Serializable
    @SerialName("DiscordMessageRef")
    data class Discord(
        val messageId: Long
    ) : MessageRef

    @Serializable
    @SerialName("MinecraftMessageRef")
    data class Minecraft(
        val messageId: String
    ) : MessageRef
}
