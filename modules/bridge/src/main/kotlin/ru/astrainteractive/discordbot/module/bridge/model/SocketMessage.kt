package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed interface SocketMessage {
    val id: Long
    val route: SocketRoute

    @Transient
    val created: Instant
}
