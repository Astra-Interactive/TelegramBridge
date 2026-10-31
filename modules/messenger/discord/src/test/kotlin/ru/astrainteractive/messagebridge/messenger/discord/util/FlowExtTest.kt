@file:Suppress("FunctionNaming", "ExpressionBodyOnOneLine")

package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.messenger.discord.model.DisallowedIntentsError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlowExtTest {
    private var fallbacks = 0

    @Test
    fun GIVEN_session_rejected_for_intents_WHEN_collected_THEN_fallback_runs_and_is_reported_once(): TestResult {
        return runTest {
            val sessions = flow<String> { throw DisallowedIntentsError(IllegalStateException("4014")) }
                .fallbackOnDisallowedIntents(
                    notifyFallback = { fallbacks += 1 },
                    fallback = { flowOf("without members") }
                )
                .toList()

            assertEquals(listOf("without members"), sessions)
            assertEquals(1, fallbacks)
        }
    }

    @Test
    fun GIVEN_session_failing_for_another_reason_WHEN_collected_THEN_failure_reaches_the_collector(): TestResult {
        return runTest {
            val t = assertFailsWith<IllegalStateException> {
                flow<String> { throw IllegalStateException("Invalid token") }
                    .fallbackOnDisallowedIntents(
                        notifyFallback = { fallbacks += 1 },
                        fallback = { flowOf("without members") }
                    )
                    .toList()
            }

            assertEquals("Invalid token", t.message)
            assertEquals(0, fallbacks)
        }
    }

    @Test
    fun GIVEN_session_that_connects_WHEN_collected_THEN_no_fallback_is_used(): TestResult {
        return runTest {
            val sessions = flowOf("with members")
                .fallbackOnDisallowedIntents(
                    notifyFallback = { fallbacks += 1 },
                    fallback = { flowOf("without members") }
                )
                .toList()

            assertEquals(listOf("with members"), sessions)
            assertEquals(0, fallbacks)
        }
    }
}
