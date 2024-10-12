package ru.astrainteractive.discordbot.module.bridge.serializer

import ru.astrainteractive.discordbot.module.bridge.model.SocketBotMessageReceivedMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPingMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRequestOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketRouteMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketUpdateOnlineMessage

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

            SocketRoute.SERVER_EVENT -> SocketMessageFormat.decodeFromString(
                deserializer = SocketServerEventMessage.serializer(),
                string = string
            )

            SocketRoute.UPDATE_ONLINE -> SocketMessageFormat.decodeFromString(
                deserializer = SocketUpdateOnlineMessage.serializer(),
                string = string
            )

            SocketRoute.BOT_MESSAGE_RECEIVED -> SocketMessageFormat.decodeFromString(
                deserializer = SocketBotMessageReceivedMessage.serializer(),
                string = string
            )

            SocketRoute.REQUEST_ONLINE_LIST -> SocketMessageFormat.decodeFromString(
                deserializer = SocketRequestOnlineListMessage.serializer(),
                string = string
            )

            SocketRoute.ONLINE_LIST -> SocketMessageFormat.decodeFromString(
                deserializer = SocketOnlineListMessage.serializer(),
                string = string
            )
        }
    }
}
