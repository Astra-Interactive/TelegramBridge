@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messaging.setup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class BindCodesTest {
    private var now = Instant.fromEpochSeconds(1_700_000_000L)
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private val bindCodes = BindCodes(clock = clock)

    @Test
    fun GIVEN_issued_code_WHEN_checked_THEN_it_is_valid_and_has_eight_digits() {
        val code = bindCodes.issue { }

        assertTrue(bindCodes.isValid(code))
        assertEquals(8, code.length)
        assertTrue(code.all(Char::isDigit))
    }

    @Test
    fun GIVEN_issued_code_WHEN_consumed_twice_THEN_only_the_first_time_works() {
        val code = bindCodes.issue { }

        assertNotNull(bindCodes.consume(code))
        assertNull(bindCodes.consume(code))
        assertFalse(bindCodes.isValid(code))
    }

    @Test
    fun GIVEN_expired_code_WHEN_consumed_THEN_nothing_is_returned() {
        val code = bindCodes.issue { }
        now += BindCodes.LIFETIME + 1.minutes

        assertFalse(bindCodes.isValid(code))
        assertNull(bindCodes.consume(code))
    }

    @Test
    fun GIVEN_unknown_code_WHEN_checked_THEN_it_is_invalid() {
        assertFalse(bindCodes.isValid("00000000"))
    }
}
