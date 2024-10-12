package ru.astrainteractive.discordbot.module.bridge.model.data

import kotlinx.serialization.Serializable
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

@Serializable
data class ServerEventMessageData(
    val instance: ServerEvent
) : MessageData
