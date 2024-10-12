package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

@Serializable
internal enum class SocketRoute {
    PING, PONG, MESSAGE
}

@Serializable
internal sealed interface SocketMessage {
    val id: Long
    val route: SocketRoute

    @Transient
    val created: Instant
}

@Serializable
internal data class SocketRouteMessage(
    override val id: Long,
    override val route: SocketRoute
) : SocketMessage {
    @Transient
    override val created: Instant = Clock.System.now()
}

@Serializable
internal data class PingMessage(
    override val id: Long
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.PING

    @Transient
    override val created: Instant = Clock.System.now()
}

@Serializable
internal data class PongMessage(
    override val id: Long
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.PONG

    @Transient
    override val created: Instant = Clock.System.now()
}

@Serializable
internal data class MessageEventMessage(
    override val id: Long,
    val event: MessageEvent
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.MESSAGE

    @Transient
    override val created: Instant = Clock.System.now()
}
