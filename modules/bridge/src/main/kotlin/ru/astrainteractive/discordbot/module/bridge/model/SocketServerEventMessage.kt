package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

@Serializable
internal data class SocketServerEventMessage(
    override val id: Long,
    val event: ServerEvent
) : SocketMessage(SocketRoute.MESSAGE)
