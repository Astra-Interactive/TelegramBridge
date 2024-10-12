package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineData

@Serializable
internal data class SocketUpdateOnlineMessage(
    override val id: Long,
    val data: UpdateOnlineData
) : SocketMessage(SocketRoute.UPDATE_ONLINE)
