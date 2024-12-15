package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerLeaveMessageEvent")
data class PlayerLeaveBEvent(
    val name: String,
    val uuid: String
) : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}
