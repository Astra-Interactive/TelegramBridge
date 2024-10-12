package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal sealed class SocketMessage(val route: SocketRoute) {
    abstract val id: Long

    @Transient
    val created: Instant = Clock.System.now()
}
