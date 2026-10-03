@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messaging.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.messagebridge.messaging.fake.FakeBEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BEventChannelTest {
    @Test
    fun GIVEN_two_consumers_answering_in_one_second_WHEN_consumed_within_timeout_THEN_both_send_it_in_time() = runTest {
        val telegram = FakeBEventConsumer { _ -> delay(ANSWER_TIME) }
        val discord = FakeBEventConsumer { _ -> delay(ANSWER_TIME) }

        val isConsumed = listOf(telegram, discord).tryConsumeWithin(
            bEvent = ServerClosedBEvent,
            timeout = TIMEOUT,
            scope = backgroundScope
        )

        assertTrue(isConsumed)
        assertEquals(ANSWER_TIME, currentTime.milliseconds)
        assertEquals(listOf<BEvent>(ServerClosedBEvent), telegram.sent)
        assertEquals(listOf<BEvent>(ServerClosedBEvent), discord.sent)
    }

    @Test
    fun GIVEN_silent_consumer_WHEN_consumed_within_timeout_THEN_returns_false_at_timeout_while_others_send() = runTest {
        val silent = FakeBEventConsumer { _ -> awaitCancellation() }
        val telegram = FakeBEventConsumer { _ -> delay(ANSWER_TIME) }

        val isConsumed = listOf(silent, telegram).tryConsumeWithin(
            bEvent = ServerClosedBEvent,
            timeout = TIMEOUT,
            scope = backgroundScope
        )

        assertFalse(isConsumed)
        assertEquals(TIMEOUT, currentTime.milliseconds)
        assertEquals(listOf<BEvent>(ServerClosedBEvent), telegram.sent)
    }

    @Test
    fun GIVEN_consumer_ignoring_cancellation_WHEN_consumed_within_timeout_THEN_returns_false_at_timeout() = runTest {
        val stuck = FakeBEventConsumer { _ -> withContext(NonCancellable) { delay(1.hours) } }

        val isConsumed = listOf(stuck).tryConsumeWithin(
            bEvent = ServerClosedBEvent,
            timeout = TIMEOUT,
            scope = backgroundScope
        )

        assertFalse(isConsumed)
        assertEquals(TIMEOUT, currentTime.milliseconds)
    }

    @Test
    fun GIVEN_failing_consumer_WHEN_consumed_within_timeout_THEN_others_still_send_it_and_it_returns_true() = runTest {
        val failing = FakeBEventConsumer { _ -> throw IllegalStateException("Telegram is offline") }
        val discord = FakeBEventConsumer { _ -> delay(ANSWER_TIME) }

        val isConsumed = listOf(failing, discord).tryConsumeWithin(
            bEvent = ServerClosedBEvent,
            timeout = TIMEOUT,
            scope = backgroundScope
        )

        assertTrue(isConsumed)
        assertTrue(failing.sent.isEmpty())
        assertEquals(listOf<BEvent>(ServerClosedBEvent), discord.sent)
    }

    private companion object {
        val TIMEOUT = 5.seconds
        val ANSWER_TIME = 1.seconds
    }
}
