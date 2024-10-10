package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface SocketMessage {
    val id: Long

    @Serializable
    data class Ping(override val id: Long) : SocketMessage

    @Serializable
    data class Pong(override val id: Long) : SocketMessage

    @Serializable
    data class Data(
        override val id: Long,
        val data: MessageData
    ) : SocketMessage
}
