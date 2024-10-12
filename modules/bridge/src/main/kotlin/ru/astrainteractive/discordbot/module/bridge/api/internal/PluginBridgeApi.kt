package ru.astrainteractive.discordbot.module.bridge.api.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData

class PluginBridgeApi : BridgeApi {
    private val sharedFlow = MutableSharedFlow<MessageData>()

    override suspend fun broadcastEvent(event: MessageData) {
        sharedFlow.emit(event)
    }

    override fun eventFlow(): Flow<MessageData> {
        return sharedFlow.asSharedFlow()
    }
}
