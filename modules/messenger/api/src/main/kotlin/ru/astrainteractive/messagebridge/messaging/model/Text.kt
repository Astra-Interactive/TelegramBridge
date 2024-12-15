package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TextMessageEvent")
sealed interface Text : BEvent {
    val author: String
    val text: String

    @Serializable
    @SerialName("TelegramTextMessageEvent")
    data class Telegram(
        override val author: String,
        override val text: String,
        val authorId: Long
    ) : Text {
        override val from: MessageFrom = MessageFrom.TELEGRAM
    }

    @Serializable
    @SerialName("DiscordTextMessageEvent")
    data class Discord(
        override val author: String,
        override val text: String,
        val authorId: Long
    ) : Text {
        override val from: MessageFrom = MessageFrom.DISCORD
    }

    @Serializable
    @SerialName("MinecraftTextMessageEvent")
    data class Minecraft(
        override val author: String,
        val uuid: String,
        override val text: String
    ) : Text {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }
}
