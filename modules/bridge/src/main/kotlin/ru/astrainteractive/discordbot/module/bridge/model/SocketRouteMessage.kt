package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SocketRouteMessage(
    val id: Long,
    val route: SocketRoute
)
