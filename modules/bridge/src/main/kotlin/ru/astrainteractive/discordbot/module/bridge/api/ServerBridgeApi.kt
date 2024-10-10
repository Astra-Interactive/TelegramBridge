package ru.astrainteractive.discordbot.module.bridge.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketServer
import ru.astrainteractive.discordbot.module.bridge.model.MessageEventData
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.broadcast
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

internal class ServerBridgeApi(private val server: WebSocketServer) : BridgeApi {
    override suspend fun broadcastEvent(event: MessageEvent) {
        val message = SocketMessage.Data(
            id = -1,
            data = MessageEventData(event)
        )
        server.broadcast(message)
    }

    override fun eventFlow(): Flow<MessageEvent> {
        return server.messageFlow.filterIsInstance<SocketMessage.Data>()
            .map { it.data }
            .filterIsInstance<MessageEventData>()
            .map { it.event }
    }
}
