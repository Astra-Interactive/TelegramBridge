package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MessageEvent")
sealed interface MessageEvent {
    @Serializable
    enum class MessageFrom(val short: String) {
        MINECRAFT("MC"), TELEGRAM("TG"), DISCORD("DS")
    }

    @Serializable
    val from: MessageFrom

    @Serializable
    data object ServerClosed : MessageEvent {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }

    @Serializable
    data object ServerOpen : MessageEvent {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }

    @Serializable
    @SerialName("TextMessageEvent")
    sealed interface Text : MessageEvent {
        val author: String
        val text: String

        @Serializable
        @SerialName("TelegramTextMessageEvent")
        data class Telegram(
            override val author: String,
            override val text: String
        ) : Text {
            override val from: MessageFrom = MessageFrom.TELEGRAM
        }

        @Serializable
        @SerialName("DiscordTextMessageEvent")
        data class Discord(
            override val author: String,
            override val text: String
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

    @Serializable
    @SerialName("PlayerJoinedMessageEvent")
    data class PlayerJoined(
        val name: String,
        val uuid: String
    ) : MessageEvent {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }

    @Serializable
    @SerialName("PlayerLeaveMessageEvent")
    data class PlayerLeave(
        val name: String,
        val uuid: String
    ) : MessageEvent {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }

    @Serializable
    @SerialName("PlayerDeathMessageEvent")
    data class PlayerDeath(
        val name: String,
        val uuid: String,
        val cause: String? = null
    ) : MessageEvent {
        override val from: MessageFrom = MessageFrom.MINECRAFT
    }
}
