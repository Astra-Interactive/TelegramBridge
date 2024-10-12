package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListData

@Serializable
internal data class SocketOnlineListMessage(
    override val id: Long,
    val data: OnlineListData
) : SocketMessage(SocketRoute.ONLINE_LIST)
