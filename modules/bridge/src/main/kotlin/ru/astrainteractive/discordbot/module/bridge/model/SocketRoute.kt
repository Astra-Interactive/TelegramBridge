package ru.astrainteractive.discordbot.module.bridge.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class SocketRoute {
    PING,
    PONG,
    MESSAGE,
    UPDATE_ONLINE,
    BOT_MESSAGE_RECEIVED,
    REQUEST_ONLINE_LIST,
    ONLINE_LIST
}
