package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.RequestOnlineMessageData

@Serializable
internal data class SocketRequestOnlineListMessage(
    override val id: Long,
) : SocketMessage(SocketRoute.REQUEST_ONLINE_LIST) {
    override val data = RequestOnlineMessageData
}
