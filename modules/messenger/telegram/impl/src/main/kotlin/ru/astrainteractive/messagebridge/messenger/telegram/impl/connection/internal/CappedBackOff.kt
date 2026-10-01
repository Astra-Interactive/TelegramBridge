package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal

import org.telegram.telegrambots.longpolling.interfaces.BackOff
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Exponential backoff whose interval is capped instead of collapsing into a long sleep.
 *
 * The library default, `ExponentialBackOff`, starts returning its `maxElapsedTimeMillis`
 * (15 minutes) once that much time has passed in errors, so a long outage leaves the
 * bridge polling once per 15 minutes even after the network is back.
 *
 * @param randomizationFactor spread around the interval as a fraction in the range
 * 0.0..1.0, so that several servers do not retry in lockstep after a shared outage.
 */
internal class CappedBackOff(
    private val initialInterval: Duration,
    private val maxInterval: Duration,
    private val multiplier: Double,
    private val randomizationFactor: Double,
    private val random: Random,
) : BackOff {
    private val currentInterval = AtomicReference(initialInterval)

    private fun Duration.randomized(): Duration {
        val delta = this * randomizationFactor
        val minMillis = (this - delta).coerceAtLeast(Duration.ZERO).inWholeMilliseconds
        val maxMillis = (this + delta).inWholeMilliseconds
        if (maxMillis <= minMillis) return this
        return random.nextLong(from = minMillis, until = maxMillis).milliseconds
    }

    override fun reset() {
        currentInterval.set(initialInterval)
    }

    /**
     * @return the current interval with [randomizationFactor] applied; milliseconds are
     * the unit [BackOff] requires, the interval itself is kept as a [Duration].
     */
    override fun nextBackOffMillis(): Long {
        val current = currentInterval.getAndUpdate { previous ->
            (previous * multiplier).coerceAtMost(maxInterval)
        }
        return current.randomized().inWholeMilliseconds
    }
}
