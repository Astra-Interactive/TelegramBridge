package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class SocketRouteMessage(
    override val id: Long,
    override val route: SocketRoute
) : SocketMessage {
    @Transient
    override val created: Instant = Clock.System.now()
}
