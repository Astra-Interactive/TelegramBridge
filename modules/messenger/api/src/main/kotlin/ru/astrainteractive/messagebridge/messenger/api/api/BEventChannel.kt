package ru.astrainteractive.messagebridge.messenger.api.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

interface BEventConsumer {
    suspend fun consume(bEvent: BEvent)
}

interface BEventReceiver {
    fun bEvents(scope: CoroutineScope): Flow<BEvent>
}

interface BEventChannel : BEventConsumer, BEventReceiver
