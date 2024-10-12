package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

internal class ClientBridgeApi(private val client: WebSocketClient) : BridgeApi {
    override suspend fun broadcastEvent(event: ServerEvent) {
        client.send(SocketRoute.MESSAGE, event)
    }

    override fun eventFlow(): Flow<ServerEvent> {
        return client.messageFlow
            .filterIsInstance<SocketServerEventMessage>()
            .map { it.event }
    }
}
