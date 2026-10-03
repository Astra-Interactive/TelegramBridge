package ru.astrainteractive.messagebridge.messaging.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun <T> Flow<T>.withRetry(
    retries: Long = 5,
    delay: Duration = 500.milliseconds,
): Flow<T> {
    return retry(retries = retries) { t ->
        println(t.stackTraceToString())
        delay(delay)
        true
    }
}
