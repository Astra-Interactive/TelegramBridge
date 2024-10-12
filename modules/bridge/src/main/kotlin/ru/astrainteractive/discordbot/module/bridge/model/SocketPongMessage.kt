package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SocketPongMessage(
    override val id: Long
) : SocketMessage(SocketRoute.PONG)
