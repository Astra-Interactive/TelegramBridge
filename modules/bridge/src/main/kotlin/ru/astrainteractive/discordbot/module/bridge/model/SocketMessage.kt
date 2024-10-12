package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData

@Serializable
internal sealed class SocketMessage(val route: SocketRoute) {
    abstract val id: Long
    abstract val data: MessageData

    @Transient
    val created: Instant = Clock.System.now()
}
