package ru.astrainteractive.messagebridge.messaging.internal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent

object BEventChannel : BEventConsumer, BEventReceiver {
    private val channel = Channel<BEvent>()

    override val bEvents: Flow<BEvent> = channel.receiveAsFlow()

    override suspend fun consume(bEvent: BEvent) {
        channel.send(bEvent)
    }
}
