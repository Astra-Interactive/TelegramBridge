package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketServer
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketRouteFactory

internal class ServerBridgeApi(private val server: WebSocketServer) : BridgeApi {
    override suspend fun broadcastEvent(data: MessageData) {
        server.broadcast(SocketRouteFactory.toRoute(data), data)
    }

    override fun eventFlow(): Flow<MessageData> {
        return server.messageFlow.map { it.data }
    }
}
