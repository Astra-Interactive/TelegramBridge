package ru.astrainteractive.messagebridge.messaging.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retry
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun <T> Flow<T>.withRetry(
    logger: Logger,
    retries: Long = 5,
    delay: Duration = 500.milliseconds,
): Flow<T> {
    return retry(retries = retries) { t ->
        logger.error(t) { "#withRetry attempt failed, retrying in $delay" }
        delay(delay)
        true
    }
}
