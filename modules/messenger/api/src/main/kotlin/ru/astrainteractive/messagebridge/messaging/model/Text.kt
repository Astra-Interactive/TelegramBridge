package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TextMessageEvent")
sealed interface Text : BEvent {
    val author: String
    val text: String

    /** The message this one answers in Telegram or Discord; null when it answers nothing. */
    val reply: Reply?

    /**
     * @property authorId id of [author] in the messenger of the enclosing [Text]; null when the bridge itself sent
     * the replied message, so [author] is already the name of whoever wrote it
     * @property text text of the replied message, or the part of it the user quoted; empty for media without a
     * caption
     */
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
