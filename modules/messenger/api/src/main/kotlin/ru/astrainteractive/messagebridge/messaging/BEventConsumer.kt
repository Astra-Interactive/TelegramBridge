package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.messaging.model.BEvent

interface BEventConsumer : Logger {
    suspend fun consume(bEvent: BEvent)
}

suspend fun BEventConsumer.tryConsume(bEvent: BEvent) {
    supervisorScope {
        launch {
            flow {
                consume(bEvent)
                emit(Unit)
            }.retry(
                retries = 5,
                predicate = {
                    error(it) { "#tryConsume could not send $bEvent. Retrying" }
                    delay(200L)
                    true
                }
            )
                .catch { error(it) { "#tryConsume could not send $bEvent" } }
                .collect()
        }
    }
}
