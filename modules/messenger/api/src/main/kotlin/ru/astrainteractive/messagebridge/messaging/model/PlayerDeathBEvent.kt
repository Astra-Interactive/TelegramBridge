package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PlayerDeathMessageEvent")
data class PlayerDeathBEvent(
    val name: String,
    val uuid: String,
    val cause: String? = null
) : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}
