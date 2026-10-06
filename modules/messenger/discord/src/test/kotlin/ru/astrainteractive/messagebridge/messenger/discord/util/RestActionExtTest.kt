@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.requests.RestAction
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRequestCancelledError
import ru.astrainteractive.messagebridge.messenger.discord.util.await
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class RestActionExtTest {
    private fun failingAction(failure: Throwable): RestAction<Unit> = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args[1]
                    ?.tryCast<Consumer<Throwable>>()
                    ?.accept(failure)
            }
        )
    )

    @Test
    fun GIVEN_request_that_jda_cancels_WHEN_awaited_THEN_it_fails_as_a_cancelled_request() = runTest {
        val action = failingAction(CancellationException("RestAction has been cancelled"))

        assertFailsWith<DiscordRequestCancelledError> { action.await() }
    }

    @Test
    fun GIVEN_request_that_never_answers_WHEN_the_caller_times_out_THEN_it_gets_a_timeout_not_a_cancelled_request() =
        runTest {
            val action: RestAction<Unit> = jdaFake(mapOf("queue" to null))

            assertFailsWith<TimeoutCancellationException> { withTimeout(1.seconds) { action.await() } }
        }

    @Test
    fun GIVEN_request_that_discord_rejects_WHEN_awaited_THEN_the_caller_gets_that_rejection() = runTest {
        val action = failingAction(IllegalStateException("10007: Unknown Member"))

        val t = assertFailsWith<IllegalStateException> { action.await() }

        assertEquals("10007: Unknown Member", t.message)
    }
}
