package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData

@Serializable
internal data class SocketServerEventMessage(
    override val id: Long,
    override val data: ServerEventMessageData
) : SocketMessage(SocketRoute.MESSAGE)
