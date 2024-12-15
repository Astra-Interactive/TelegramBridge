package ru.astrainteractive.messagebridge.messaging.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent

object BEventChannel : BEventConsumer, BEventReceiver {
    private val channel = MutableSharedFlow<BEvent>(1)

    override val bEvents: Flow<BEvent> = channel.asSharedFlow()

    override suspend fun consume(bEvent: BEvent) {
        channel.emit(bEvent)
    }
}
