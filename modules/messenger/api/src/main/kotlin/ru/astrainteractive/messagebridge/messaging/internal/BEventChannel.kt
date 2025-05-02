package ru.astrainteractive.messagebridge.messaging.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent

object BEventChannel :
    BEventConsumer,
    BEventReceiver,
    Logger by JUtiltLogger("MessageBridge-BEventChannel") {
    private val channel = MutableSharedFlow<BEvent>(1)

    override fun bEvents(scope: CoroutineScope): Flow<BEvent> = channel
        .asSharedFlow()
        .transform { event ->
            emit(event)
            kotlinx.coroutines.delay(DELAY_MILLIS)
        }.shareIn(scope, SharingStarted.Lazily)

    override suspend fun consume(bEvent: BEvent) {
        channel.emit(bEvent)
    }

    /**
     * When people write a lot of messages at one time - we can
     * encounter timeout for discord/tg api, so we need to wait a little
     */
    private const val DELAY_MILLIS = 500L
}
