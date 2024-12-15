package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerJoinedMessageEvent")
data class PlayerJoinedBEvent(
    val name: String,
    val uuid: String,
    val hasPlayedBefore: Boolean
) : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}
