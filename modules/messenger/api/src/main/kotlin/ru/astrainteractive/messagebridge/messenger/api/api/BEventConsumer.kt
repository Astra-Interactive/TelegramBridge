package ru.astrainteractive.messagebridge.messenger.api.api

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.util.withRetry

interface BEventConsumer : Logger {
    suspend fun consume(bEvent: BEvent)
}

suspend fun BEventConsumer.tryConsume(bEvent: BEvent) {
    supervisorScope {
        launch {
            flow {
                consume(bEvent)
                emit(Unit)
            }.withRetry(this@tryConsume)
                .catch { t -> error(t) { "#tryConsume could not send $bEvent" } }
                .collect()
        }
    }
}
