package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.PingMessageData

@Serializable
internal data class SocketPingMessage(
    override val id: Long
) : SocketMessage(SocketRoute.PING) {
    override val data = PingMessageData
}
