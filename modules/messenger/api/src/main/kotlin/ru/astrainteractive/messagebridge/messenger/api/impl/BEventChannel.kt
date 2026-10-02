package ru.astrainteractive.messagebridge.messenger.api.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import kotlin.time.Duration.Companion.milliseconds

class BEventChannel :
    BEventConsumer,
    Logger by JUtiltLogger("MessageBridge-BEventChannel") {
    private val channel = MutableSharedFlow<BEvent>(1)

    fun bEvents(scope: CoroutineScope): Flow<BEvent> = channel
        .asSharedFlow()
        .transform { event ->
            emit(event)
            delay(SPACING)
        }
        .shareIn(scope, SharingStarted.Lazily)

    override suspend fun consume(bEvent: BEvent) {
        channel.emit(bEvent)
    }

    private companion object {
        /**
         * When people write a lot of messages at one time - we can
         * encounter timeout for discord/tg api, so we need to wait a little
         */
        val SPACING = 500.milliseconds
    }
}
