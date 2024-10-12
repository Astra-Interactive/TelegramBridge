package ru.astrainteractive.discordbot.module.bridge.serializer

import ru.astrainteractive.discordbot.module.bridge.model.MessageEventMessage
import ru.astrainteractive.discordbot.module.bridge.model.PingMessage
import ru.astrainteractive.discordbot.module.bridge.model.PongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

internal object SocketMessageFactory {
    fun <T> create(
        route: SocketRoute,
        data: T? = null,
        getId: () -> Long
    ): SocketMessage {
        return when (route) {
            SocketRoute.PING -> PingMessage(id = getId.invoke())
            SocketRoute.PONG -> PongMessage(id = getId.invoke())
            SocketRoute.MESSAGE -> MessageEventMessage(
                id = getId.invoke(),
                event = data as MessageEvent
            )
        }
    }
}
