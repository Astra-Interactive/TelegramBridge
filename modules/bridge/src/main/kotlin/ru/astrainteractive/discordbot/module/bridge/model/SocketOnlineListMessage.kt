package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.OnlineListMessageData

@Serializable
internal data class SocketOnlineListMessage(
    override val id: Long,
    override val data: OnlineListMessageData
) : SocketMessage(SocketRoute.ONLINE_LIST)
