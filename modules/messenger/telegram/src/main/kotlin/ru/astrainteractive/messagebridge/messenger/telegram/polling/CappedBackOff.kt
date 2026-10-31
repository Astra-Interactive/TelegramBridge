@file:Suppress("OperatorBeforeTrailingCall")

package ru.astrainteractive.messagebridge.messenger.telegram.polling

import org.telegram.telegrambots.longpolling.interfaces.BackOff
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

// The library default, ExponentialBackOff, starts returning its maxElapsedTimeMillis (15 minutes) once that much
// time has passed in errors, so a long outage leaves the bridge polling once per 15 minutes even after the network
// is back.
internal class CappedBackOff(
    private val initialInterval: Duration = INITIAL_INTERVAL,
    private val maxInterval: Duration = MAX_INTERVAL,
    private val multiplier: Double = MULTIPLIER,
    private val randomizationFactor: Double = RANDOMIZATION_FACTOR
) : BackOff {
    private val currentInterval = AtomicReference(initialInterval)

    private fun Duration.randomized(): Duration {
        val delta = this * randomizationFactor
        val min = (this - delta).coerceAtLeast(Duration.ZERO)
        val max = this + delta
        if (max <= min) return this
        return Random
            .nextLong(from = min.inWholeMilliseconds, until = max.inWholeMilliseconds)
            .milliseconds
    }

    override fun reset() {
        currentInterval.set(initialInterval)
    }

    override fun nextBackOffMillis(): Long {
        val current = currentInterval.getAndUpdate { previous ->
            (previous * multiplier).coerceAtMost(maxInterval)
        }
        return current.randomized().inWholeMilliseconds
    }

    private companion object {
        val INITIAL_INTERVAL = 500.milliseconds
        val MAX_INTERVAL = 1.minutes
        const val MULTIPLIER = 1.5
        const val RANDOMIZATION_FACTOR = 0.5
    }
}
