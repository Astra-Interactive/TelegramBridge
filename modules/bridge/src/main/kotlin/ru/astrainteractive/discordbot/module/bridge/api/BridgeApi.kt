package ru.astrainteractive.discordbot.module.bridge.api

import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.discordbot.module.bridge.model.data.MessageData

interface BridgeApi {
    suspend fun broadcastEvent(data: MessageData)

    fun eventFlow(): Flow<MessageData>
}
