@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.connection

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CappedBackOffTest {
    private fun backOffOf(randomizationFactor: Double) = CappedBackOff(
        initialInterval = 500.milliseconds,
        maxInterval = 2.seconds,
        multiplier = 2.0,
        randomizationFactor = randomizationFactor,
        random = Random(seed = 42)
    )

    @Test
    fun GIVEN_failures_in_a_row_WHEN_backing_off_THEN_interval_grows_up_to_the_cap() {
        val backOff = backOffOf(randomizationFactor = 0.0)

        val intervals = List(5) { _ -> backOff.nextBackOffMillis() }

        assertEquals(listOf(500L, 1000L, 2000L, 2000L, 2000L), intervals)
    }

    @Test
    fun GIVEN_long_outage_WHEN_it_ends_and_backoff_is_reset_THEN_interval_starts_over() {
        val backOff = backOffOf(randomizationFactor = 0.0)
        repeat(10) { _ -> backOff.nextBackOffMillis() }

        backOff.reset()

        assertEquals(500L, backOff.nextBackOffMillis())
    }

    @Test
    fun GIVEN_randomization_WHEN_backing_off_THEN_interval_stays_around_the_current_one() {
        val backOff = backOffOf(randomizationFactor = 0.5)

        val intervals = List(100) { _ -> backOff.nextBackOffMillis() }

        assertTrue(intervals.first() in 250L until 750L, "${intervals.first()}")
        assertTrue(intervals.drop(2).all { interval -> interval in 1000L until 3000L }, "$intervals")
        assertTrue(intervals.distinct().size > 1, "$intervals")
    }
}
