package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class SocketRequestOnlineListMessage(
    override val id: Long
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.REQUEST_ONLINE_LIST

    @Transient
    override val created: Instant = Clock.System.now()
}
