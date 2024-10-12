package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineData

@Serializable
internal data class SocketUpdateOnlineMessage(
    override val id: Long,
    val data: UpdateOnlineData
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.UPDATE_ONLINE

    @Transient
    override val created: Instant = Clock.System.now()
}
