package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

@Serializable
internal data class SocketServerEventMessage(
    override val id: Long,
    val event: ServerEvent
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.MESSAGE

    @Transient
    override val created: Instant = Clock.System.now()
}
