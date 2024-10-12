package ru.astrainteractive.discordbot.module.bridge.api

import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

interface BridgeApi {
    suspend fun broadcastEvent(event: ServerEvent)

    fun eventFlow(): Flow<ServerEvent>
}
