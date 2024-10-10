package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

@Serializable
@SerialName("MessageData")
internal sealed interface MessageData

@Serializable
@SerialName("StringMessageData")
internal data class StringMessageData(val value: String) : MessageData

@Serializable
@SerialName("MessageEventData")
internal data class MessageEventData(val event: MessageEvent) : MessageData
