package ru.astrainteractive.messagebridge.messaging.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MessageEvent")
sealed interface BEvent {

    @Serializable
    val from: MessageFrom
}
