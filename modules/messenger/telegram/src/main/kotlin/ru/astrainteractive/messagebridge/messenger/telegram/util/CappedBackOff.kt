package ru.astrainteractive.messagebridge.messenger.telegram.util

import org.telegram.telegrambots.longpolling.interfaces.BackOff
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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
    private val initialInterval: Duration = INITIAL_INTERVAL,
    private val maxInterval: Duration = MAX_INTERVAL,
    private val multiplier: Double = MULTIPLIER,
    private val randomizationFactor: Double = RANDOMIZATION_FACTOR
) : BackOff {
    private val currentInterval = AtomicReference(initialInterval)

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

    private fun Duration.randomized(): Duration {
        val delta = this * randomizationFactor
        val min = (this - delta).coerceAtLeast(Duration.ZERO)
        val max = this + delta
        if (max <= min) return this
        return Random
            .nextLong(from = min.inWholeMilliseconds, until = max.inWholeMilliseconds)
            .milliseconds
    }

    private companion object {
        val INITIAL_INTERVAL = 500.milliseconds
        val MAX_INTERVAL = 1.minutes
        const val MULTIPLIER = 1.5
        const val RANDOMIZATION_FACTOR = 0.5
    }
}
