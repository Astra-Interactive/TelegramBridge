package ru.astrainteractive.discordbot.module.bridge.api

import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

interface BridgeApi {
    suspend fun broadcastEvent(event: MessageEvent)

    fun eventFlow(): Flow<MessageEvent>
}
