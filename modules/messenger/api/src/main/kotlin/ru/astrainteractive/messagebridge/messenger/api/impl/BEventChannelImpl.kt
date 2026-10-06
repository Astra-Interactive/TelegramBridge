package ru.astrainteractive.messagebridge.messenger.api.impl

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

class BEventChannelImpl :
    BEventChannel,
    Logger by JUtiltLogger("MessageBridge-BEventChannelImpl") {
    private val eventsChannel = MutableSharedFlow<BEvent>(1)

    override fun receiveAsFlow(): Flow<BEvent> = eventsChannel
        .asSharedFlow()
        .buffer(capacity = RECEIVER_BUFFER_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun consume(bEvent: BEvent) {
        eventsChannel.emit(bEvent)
    }

    private companion object {
        const val RECEIVER_BUFFER_CAPACITY = 64
    }
}
