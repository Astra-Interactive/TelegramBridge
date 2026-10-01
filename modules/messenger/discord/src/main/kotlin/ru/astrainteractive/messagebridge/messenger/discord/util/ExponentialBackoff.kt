package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Doubles the delay after every failed attempt, up to [max], so a long outage is not retried every few seconds. */
internal class ExponentialBackoff(
    private val initial: Duration = INITIAL,
    private val max: Duration = MAX
) {
    /** @param attempt number of failed attempts before this one, starting from 0 */
    fun delayFor(attempt: Int): Duration {
        val multiplier = 1L shl attempt.coerceIn(0, MAX_SHIFT)
        return (initial * multiplier.toDouble()).coerceAtMost(max)
    }

    private companion object {
        val INITIAL = 5.seconds
        val MAX = 5.minutes
        const val MAX_SHIFT = 30
    }
}
