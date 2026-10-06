package ru.astrainteractive.messagebridge.messenger.api.api

import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

interface BEventConsumer {
    suspend fun consume(bEvent: BEvent)
}

interface BEventReceiver {
    fun receiveAsFlow(): Flow<BEvent>
}

interface BEventChannel : BEventConsumer, BEventReceiver
