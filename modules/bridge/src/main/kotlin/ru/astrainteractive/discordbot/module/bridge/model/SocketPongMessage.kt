package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.PongMessageData

@Serializable
internal data class SocketPongMessage(
    override val id: Long
) : SocketMessage(SocketRoute.PONG) {
    override val data = PongMessageData
}
