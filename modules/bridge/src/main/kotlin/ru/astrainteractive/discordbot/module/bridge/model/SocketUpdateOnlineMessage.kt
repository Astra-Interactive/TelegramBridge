package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineMessageData

@Serializable
internal data class SocketUpdateOnlineMessage(
    override val id: Long,
    override val data: UpdateOnlineMessageData
) : SocketMessage(SocketRoute.UPDATE_ONLINE)
