package ru.astrainteractive.discordbot.module.bridge.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.model.MessageEventData
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

internal class ClientBridgeApi(private val client: WebSocketClient) : BridgeApi {
    override suspend fun broadcastEvent(event: MessageEvent) {
        client.send(MessageEventData(event))
    }

    override fun eventFlow(): Flow<MessageEvent> {
        return client.messageFlow.filterIsInstance<SocketMessage.Data>()
            .map { it.data }
            .filterIsInstance<MessageEventData>()
            .map { it.event }
    }
}
