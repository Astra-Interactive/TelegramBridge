package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SocketRequestOnlineListMessage(
    override val id: Long
) : SocketMessage(SocketRoute.REQUEST_ONLINE_LIST)
