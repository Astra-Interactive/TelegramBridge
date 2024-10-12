package ru.astrainteractive.discordbot.module.bridge.serializer

import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPingMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

internal object SocketMessageFactory {
    fun <T> create(
        route: SocketRoute,
        data: T? = null,
        getId: () -> Long
    ): SocketMessage {
        return when (route) {
            SocketRoute.PING -> SocketPingMessage(id = getId.invoke())
            SocketRoute.PONG -> SocketPongMessage(id = getId.invoke())
            SocketRoute.MESSAGE -> SocketServerEventMessage(
                id = getId.invoke(),
                event = data as ServerEvent
            )
        }
    }
}
