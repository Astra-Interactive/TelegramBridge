package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SocketPingMessage(
    override val id: Long
) : SocketMessage(SocketRoute.PING)
