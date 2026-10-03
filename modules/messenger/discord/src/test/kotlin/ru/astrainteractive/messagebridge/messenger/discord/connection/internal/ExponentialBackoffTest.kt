@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.connection.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffTest {
    private val backoff = ExponentialBackoff(initial = 5.seconds, max = 5.minutes)

    @Test
    fun GIVEN_first_failures_WHEN_delay_THEN_it_doubles() {
        assertEquals(listOf(5.seconds, 10.seconds, 20.seconds, 40.seconds), (0..3).map(backoff::delayFor))
    }

    @Test
    fun GIVEN_long_outage_WHEN_delay_THEN_it_stays_at_max() {
        assertEquals(5.minutes, backoff.delayFor(7))
        assertEquals(5.minutes, backoff.delayFor(Int.MAX_VALUE))
    }
}
