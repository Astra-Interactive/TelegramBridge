package ru.astrainteractive.messagebridge.messenger.discord.connection.internal

import kotlin.time.Duration

internal class ExponentialBackoff(
    private val initial: Duration,
    private val max: Duration
) {
    fun delayFor(attempt: Int): Duration {
        val multiplier = 1L shl attempt.coerceIn(0, MAX_SHIFT)
        return (initial * multiplier.toDouble()).coerceAtMost(max)
    }

    private companion object {
        const val MAX_SHIFT = 30
    }
}
