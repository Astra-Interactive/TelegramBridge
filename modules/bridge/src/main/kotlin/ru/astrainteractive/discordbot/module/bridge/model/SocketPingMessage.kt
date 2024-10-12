package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class SocketPingMessage(
    override val id: Long
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.PING

    @Transient
    override val created: Instant = Clock.System.now()
}
