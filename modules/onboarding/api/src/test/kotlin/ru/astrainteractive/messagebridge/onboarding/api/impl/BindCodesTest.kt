@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.api.impl

import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class BindCodesTest {
    private val clock = FakeClock(now = Instant.fromEpochSeconds(1_700_000_000L))
    private val lifetime = 10.minutes
    private val bindCodes = BindCodes(clock = clock, lifetime = lifetime, random = Random(SEED))

    @Test
    fun GIVEN_issued_code_WHEN_checked_THEN_it_is_valid_has_eight_digits_and_its_lifetime() {
        val code = bindCodes.issue { _ -> }

        assertTrue(bindCodes.isValid(code.value))
        assertEquals(8, code.value.length)
        assertTrue(code.value.all(Char::isDigit))
        assertEquals(lifetime, code.lifetime)
    }

    @Test
    fun GIVEN_issued_code_WHEN_consumed_twice_THEN_only_the_first_time_works() {
        val code = bindCodes.issue { _ -> }

        assertNotNull(bindCodes.consume(code.value))
        assertNull(bindCodes.consume(code.value))
        assertFalse(bindCodes.isValid(code.value))
    }

    @Test
    fun GIVEN_expired_code_WHEN_consumed_THEN_nothing_is_returned() {
        val code = bindCodes.issue { _ -> }
        clock.now += lifetime + 1.minutes

        assertFalse(bindCodes.isValid(code.value))
        assertNull(bindCodes.consume(code.value))
    }

    @Test
    fun GIVEN_code_on_the_last_moment_of_its_lifetime_WHEN_consumed_THEN_it_still_works() {
        val code = bindCodes.issue { _ -> }
        clock.now += lifetime

        assertNotNull(bindCodes.consume(code.value))
    }

    @Test
    fun GIVEN_two_issued_codes_WHEN_one_is_consumed_THEN_the_other_stays_valid() {
        val first = bindCodes.issue { _ -> }
        val second = bindCodes.issue { _ -> }

        assertNotEquals(first.value, second.value)
        bindCodes.consume(first.value)
        assertTrue(bindCodes.isValid(second.value))
    }

    @Test
    fun GIVEN_unknown_code_WHEN_checked_THEN_it_is_invalid() {
        assertFalse(bindCodes.isValid("00000000"))
    }

    private companion object {
        const val SEED = 42L
    }
}
