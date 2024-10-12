package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketRouteFactory

internal class ClientBridgeApi(private val client: WebSocketClient) : BridgeApi {
    override suspend fun broadcastEvent(data: MessageData) {
        client.send(SocketRouteFactory.toRoute(data), data)
    }

    override fun eventFlow(): Flow<MessageData> {
        return client.messageFlow.map { it.data }
    }
}
