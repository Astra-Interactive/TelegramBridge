package ru.astrainteractive.messagebridge.messaging.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.BEvent

class BEventChannelImpl :
    BEventChannel,
    Logger by JUtiltLogger("MessageBridge-BEventChannelImpl").withoutParentHandlers() {
    private val channel = MutableSharedFlow<BEvent>(1)

    override fun bEvents(scope: CoroutineScope): Flow<BEvent> {
        val receiverBuffer = Channel<BEvent>(RECEIVER_BUFFER_CAPACITY, BufferOverflow.DROP_OLDEST) { bEvent ->
            warn { "#bEvents a receiver fell behind, dropped $bEvent" }
        }
        channel
            .onEach(receiverBuffer::send)
            .launchIn(scope)
        return receiverBuffer.receiveAsFlow()
    }

    override suspend fun consume(bEvent: BEvent) {
        channel.emit(bEvent)
    }

    private companion object {
        const val RECEIVER_BUFFER_CAPACITY = 64
    }
}
