package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketServer
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

internal class ServerBridgeApi(private val server: WebSocketServer) : BridgeApi {
    override suspend fun broadcastEvent(event: ServerEvent) {
        server.broadcast(SocketRoute.MESSAGE, event)
    }

    override fun eventFlow(): Flow<ServerEvent> {
        return server.messageFlow
            .filterIsInstance<SocketServerEventMessage>()
            .map { it.event }
    }
}
