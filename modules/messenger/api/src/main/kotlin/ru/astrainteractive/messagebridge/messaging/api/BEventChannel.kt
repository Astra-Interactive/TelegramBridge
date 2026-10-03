package ru.astrainteractive.messagebridge.messaging.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.util.withRetry

interface BEventConsumer : Logger {
    suspend fun consume(bEvent: BEvent)
}

interface BEventReceiver {
    fun bEvents(scope: CoroutineScope): Flow<BEvent>
}

interface BEventChannel : BEventConsumer, BEventReceiver

suspend fun BEventConsumer.tryConsume(bEvent: BEvent) {
    supervisorScope {
        launch {
            flow {
                consume(bEvent)
                emit(Unit)
            }.withRetry()
                .catch { t -> error(t) { "#tryConsume could not send $bEvent" } }
                .collect()
        }
    }
}
