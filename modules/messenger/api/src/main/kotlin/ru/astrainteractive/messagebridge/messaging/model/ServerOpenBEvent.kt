package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.Serializable

@Serializable
data object ServerOpenBEvent : BEvent {
    override val from: MessageFrom = MessageFrom.MINECRAFT
}