package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface BEventConsumer : Logger {
    suspend fun consume(bEvent: BEvent)
}

public fun <T> Flow<T>.withRetry(
    retries: Long = 5,
    delay: Duration = 500.milliseconds,
): Flow<T> {
    return retry(retries = retries) {
        println(it.stackTraceToString())
        delay(delay)
        true
    }
}

suspend fun BEventConsumer.tryConsume(bEvent: BEvent) {
    supervisorScope {
        launch {
            flow {
                consume(bEvent)
                emit(Unit)
            }.withRetry()
                .catch { error(it) { "#tryConsume could not send $bEvent" } }
                .collect()
        }
    }
}
