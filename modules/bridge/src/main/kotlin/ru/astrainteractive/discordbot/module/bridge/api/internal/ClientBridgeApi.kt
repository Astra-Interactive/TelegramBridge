package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData

internal class ClientBridgeApi(private val client: WebSocketClient) : BridgeApi {
    override suspend fun broadcastEvent(event: MessageData) {
        client.send(SocketRoute.MESSAGE, event)
    }

    override fun eventFlow(): Flow<MessageData> {
        return client.messageFlow
            .filterIsInstance<SocketMessage>()
            .map { it.data }
    }
}
