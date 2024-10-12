package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class SocketRoute {
    PING, PONG, MESSAGE
}
