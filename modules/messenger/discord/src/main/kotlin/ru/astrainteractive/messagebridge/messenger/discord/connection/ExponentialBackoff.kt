package ru.astrainteractive.messagebridge.messenger.discord.connection

import kotlin.time.Duration

/** Doubles the delay after every failed attempt, up to [max], so a long outage is not retried every few seconds. */
internal class ExponentialBackoff(
    private val initial: Duration,
    private val max: Duration
) {
    /** @param attempt number of failed attempts before this one, starting from 0 */
    fun delayFor(attempt: Int): Duration {
        val multiplier = 1L shl attempt.coerceIn(0, MAX_SHIFT)
        return (initial * multiplier.toDouble()).coerceAtMost(max)
    }

    private companion object {
        const val MAX_SHIFT = 30
    }
}
