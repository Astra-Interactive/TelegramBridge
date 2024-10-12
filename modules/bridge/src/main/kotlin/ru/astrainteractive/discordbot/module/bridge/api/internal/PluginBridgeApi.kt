package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

class PluginBridgeApi : BridgeApi {
    private val sharedFlow = MutableSharedFlow<MessageEvent>()

    override suspend fun broadcastEvent(event: MessageEvent) {
        sharedFlow.emit(event)
    }

    override fun eventFlow(): Flow<MessageEvent> {
        return sharedFlow.asSharedFlow()
    }
}
