package ru.astrainteractive.discordbot.module.bridge.serializer

import kotlinx.serialization.decodeFromString
import ru.astrainteractive.discordbot.module.bridge.model.MessageEventMessage
import ru.astrainteractive.discordbot.module.bridge.model.PingMessage
import ru.astrainteractive.discordbot.module.bridge.model.PongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessageFormat
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketRouteMessage

internal object SocketMessageSerializer {
    fun fromString(string: String): SocketMessage {
        val routeMessage = SocketMessageFormat.decodeFromString(
            deserializer = SocketRouteMessage.serializer(),
            string = string
        )
        return when (routeMessage.route) {
            SocketRoute.PING -> SocketMessageFormat.decodeFromString(
                deserializer = PingMessage.serializer(),
                string = string
            )

            SocketRoute.PONG -> SocketMessageFormat.decodeFromString(
                deserializer = PongMessage.serializer(),
                string = string
            )

            SocketRoute.MESSAGE -> SocketMessageFormat.decodeFromString(
                deserializer = MessageEventMessage.serializer(),
                string = string
            )
        }
    }
}
