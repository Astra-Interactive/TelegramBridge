package ru.astrainteractive.messagebridge.messenger.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MessageEvent")
sealed interface BEvent {

    @Serializable
    val from: MessageFrom
}

@Serializable
@SerialName("PlayerDeathMessageEvent")
data class PlayerDeathBEvent(
    val name: String,
    val uuid: String,
    val cause: String? = null
) : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}

@Serializable
@SerialName("PlayerJoinedMessageEvent")
data class PlayerJoinedBEvent(
    val name: String,
    val uuid: String,
    val hasPlayedBefore: Boolean
) : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}

@Serializable
@SerialName("PlayerLeaveMessageEvent")
data class PlayerLeaveBEvent(
    val name: String,
    val uuid: String
) : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}

@Serializable
data object ServerClosedBEvent : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}

@Serializable
data object ServerOpenBEvent : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}

@Serializable
@SerialName("TextMessageEvent")
sealed interface Text : BEvent {
    val author: String
    val text: String

    val reply: Reply?

    @Serializable
    data class Reply(
        val author: String,
        val authorId: Long?,
        val text: String
    )

    @Serializable
    @SerialName("TelegramTextMessageEvent")
    data class Telegram(
        override val author: String,
        override val text: String,
        val authorId: Long,
        override val reply: Reply?
    ) : Text {
        override val from: MessageFrom = MessageFrom.TELEGRAM
    }

    @Serializable
    @SerialName("DiscordTextMessageEvent")
    data class Discord(
        override val author: String,
        override val text: String,
        val authorId: Long,
        override val reply: Reply?
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
        override val reply: Reply? = null
    }
}
