@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messaging.api

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.messaging.model.Interception
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessageInterceptorTest {
    private val calls = mutableListOf<String>()

    private fun recording(name: String, interception: Interception): MessageInterceptor<String> {
        return MessageInterceptor { event ->
            calls += "$name:$event"
            interception
        }
    }

    @Test
    fun GIVEN_no_interceptors_WHEN_message_is_intercepted_THEN_it_passes() = runTest {
        val interceptors = emptyList<MessageInterceptor<String>>()

        assertEquals(Interception.Pass, interceptors.intercept("hello"))
    }

    @Test
    fun GIVEN_interceptors_that_all_pass_WHEN_message_is_intercepted_THEN_each_sees_it_once_in_order() = runTest {
        val interceptors = listOf(
            recording(name = "first", interception = Interception.Pass),
            recording(name = "second", interception = Interception.Pass)
        )

        val interception = interceptors.intercept("hello")

        assertEquals(Interception.Pass, interception)
        assertEquals(listOf("first:hello", "second:hello"), calls)
    }

    @Test
    fun GIVEN_first_interceptor_consumes_WHEN_message_is_intercepted_THEN_the_rest_never_see_it() = runTest {
        val interceptors = listOf(
            recording(name = "first", interception = Interception.Consumed),
            recording(name = "second", interception = Interception.Pass)
        )

        val interception = interceptors.intercept("hello")

        assertEquals(Interception.Consumed, interception)
        assertEquals(listOf("first:hello"), calls)
    }

    @Test
    fun GIVEN_first_interceptor_replies_WHEN_message_is_intercepted_THEN_its_reply_wins() = runTest {
        val interceptors = listOf(
            recording(name = "first", interception = Interception.Reply("linked")),
            recording(name = "second", interception = Interception.Reply("ignored"))
        )

        val interception = interceptors.intercept("hello")

        assertEquals(Interception.Reply("linked"), interception)
        assertEquals(listOf("first:hello"), calls)
    }

    @Test
    fun GIVEN_passing_then_replying_interceptor_WHEN_message_is_intercepted_THEN_both_run_and_reply_wins() = runTest {
        val interceptors = listOf(
            recording(name = "first", interception = Interception.Pass),
            recording(name = "second", interception = Interception.Reply("linked"))
        )

        val interception = interceptors.intercept("hello")

        assertEquals(Interception.Reply("linked"), interception)
        assertEquals(listOf("first:hello", "second:hello"), calls)
    }

    @Test
    fun GIVEN_interceptor_that_throws_WHEN_message_is_intercepted_THEN_failure_reaches_caller_and_rest_never_run() =
        runTest {
            val interceptors = listOf(
                MessageInterceptor<String> { _ -> error("Database is locked") },
                recording(name = "second", interception = Interception.Pass)
            )

            val t = assertFailsWith<IllegalStateException> { interceptors.intercept("hello") }

            assertEquals("Database is locked", t.message)
            assertEquals(emptyList(), calls)
        }
}
