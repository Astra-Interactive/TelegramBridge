package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

class PluginBridgeApi : BridgeApi {
    private val sharedFlow = MutableSharedFlow<ServerEvent>()

    override suspend fun broadcastEvent(event: ServerEvent) {
        sharedFlow.emit(event)
    }

    override fun eventFlow(): Flow<ServerEvent> {
        return sharedFlow.asSharedFlow()
    }
}
