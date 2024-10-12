package ru.astrainteractive.discordbot.module.bridge.serializer

import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPingMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketRouteMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage

internal object SocketMessageSerializer {
    fun fromString(string: String): SocketMessage {
        val routeMessage = SocketMessageFormat.decodeFromString(
            deserializer = SocketRouteMessage.serializer(),
            string = string
        )
        return when (routeMessage.route) {
            SocketRoute.PING -> SocketMessageFormat.decodeFromString(
                deserializer = SocketPingMessage.serializer(),
                string = string
            )

            SocketRoute.PONG -> SocketMessageFormat.decodeFromString(
                deserializer = SocketPongMessage.serializer(),
                string = string
            )

            SocketRoute.MESSAGE -> SocketMessageFormat.decodeFromString(
                deserializer = SocketServerEventMessage.serializer(),
                string = string
            )
        }
    }
}
