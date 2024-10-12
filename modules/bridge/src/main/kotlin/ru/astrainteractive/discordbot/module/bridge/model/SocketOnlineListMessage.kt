package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListData

@Serializable
internal data class SocketOnlineListMessage(
    override val id: Long,
    val data: OnlineListData
) : SocketMessage {
    override val route: SocketRoute = SocketRoute.ONLINE_LIST

    @Transient
    override val created: Instant = Clock.System.now()
}
