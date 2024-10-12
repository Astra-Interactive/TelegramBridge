package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.MessageEventMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

internal class ClientBridgeApi(private val client: WebSocketClient) : BridgeApi {
    override suspend fun broadcastEvent(event: MessageEvent) {
        client.send(SocketRoute.MESSAGE, event)
    }

    override fun eventFlow(): Flow<MessageEvent> {
        return client.messageFlow
            .filterIsInstance<MessageEventMessage>()
            .map { it.event }
    }
}
