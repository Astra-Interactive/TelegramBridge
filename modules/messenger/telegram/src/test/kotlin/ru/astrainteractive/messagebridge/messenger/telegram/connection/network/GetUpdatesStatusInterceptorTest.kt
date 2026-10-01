@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.connection.network

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.connection.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.failure.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.failure.model.TelegramFailure
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class GetUpdatesStatusInterceptorTest {
    private val timeSource = TestTimeSource()
    private val states: MutableList<TelegramConnectionState> = CopyOnWriteArrayList()
    private val interceptor = GetUpdatesStatusInterceptor(
        botName = BOT_NAME,
        failureMapper = TelegramFailureMapper(configFlow = MutableStateFlow(PluginConfiguration())),
        timeSource = timeSource,
        onState = { state -> states += state }
    )

    @Volatile
    private var answerCode = OK

    @Volatile
    private var isOffline = false

    private val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .addInterceptor(Interceptor { chain -> answer(chain.request()) })
        .build()

    private fun answer(request: Request): Response {
        if (isOffline) throw ConnectException("Connection refused")
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(answerCode)
            .message("")
            .body("{}".toResponseBody())
            .build()
    }

    private fun callOf(method: String): Call {
        val request = Request.Builder().url("https://api.telegram.org/bot123:token/$method").build()
        return client.newCall(request)
    }

    private fun poll(code: Int) {
        answerCode = code
        callOf(GET_UPDATES).execute().close()
    }

    private fun assertStates(vararg expected: TelegramConnectionState) {
        assertEquals(expected.toList(), states.toList())
    }

    @AfterTest
    fun shutDown() {
        client.dispatcher.executorService.shutdown()
    }

    @Test
    fun GIVEN_updates_answered_WHEN_polling_THEN_bot_is_connected() {
        poll(OK)

        assertStates(TelegramConnectionState.Connected(BOT_NAME))
    }

    @Test
    fun GIVEN_another_program_polls_WHEN_telegram_answers_conflict_THEN_token_is_in_use() {
        poll(CONFLICT)

        assertStates(TelegramConnectionState.Failed(TelegramFailure.TokenInUse))
    }

    @Test
    fun GIVEN_recent_conflict_WHEN_next_poll_succeeds_THEN_token_is_still_in_use() {
        poll(CONFLICT)
        timeSource += 1.minutes

        poll(OK)

        assertEquals(TelegramConnectionState.Failed(TelegramFailure.TokenInUse), states.last())
    }

    @Test
    fun GIVEN_old_conflict_WHEN_next_poll_succeeds_THEN_bot_is_connected_again() {
        poll(CONFLICT)
        timeSource += 2.minutes + 1.seconds

        poll(OK)

        assertEquals(TelegramConnectionState.Connected(BOT_NAME), states.last())
    }

    @Test
    fun GIVEN_revoked_token_WHEN_polling_THEN_token_is_invalid() {
        poll(UNAUTHORIZED)
        poll(NOT_FOUND)

        val invalidToken = TelegramConnectionState.Failed(TelegramFailure.InvalidToken)
        assertStates(invalidToken, invalidToken)
    }

    @Test
    fun GIVEN_server_error_WHEN_polling_THEN_state_is_kept_for_the_retry() {
        poll(BAD_GATEWAY)

        assertStates()
    }

    @Test
    fun GIVEN_other_request_WHEN_it_fails_THEN_polling_state_is_kept() {
        answerCode = CONFLICT

        callOf("sendmessage").execute().close()

        assertStates()
    }

    @Test
    fun GIVEN_network_is_down_WHEN_polling_THEN_it_is_a_network_failure() {
        isOffline = true

        assertFailsWith<IOException> { poll(OK) }

        assertStates(TelegramConnectionState.Failed(TelegramFailure.Network(null, null)))
    }

    @Test
    fun GIVEN_cancelled_poll_WHEN_it_fails_THEN_nothing_is_reported() {
        isOffline = true
        val call = callOf(GET_UPDATES)
        call.cancel()

        assertFailsWith<IOException> { call.execute() }

        assertStates()
    }

    @Test
    fun GIVEN_deactivated_interceptor_WHEN_polling_THEN_next_session_state_is_not_overwritten() {
        interceptor.deactivate()

        poll(CONFLICT)

        assertStates()
    }

    private companion object {
        const val BOT_NAME = "@MyBridgeBot"
        const val GET_UPDATES = "getUpdates"
        const val OK = 200
        const val UNAUTHORIZED = 401
        const val NOT_FOUND = 404
        const val CONFLICT = 409
        const val BAD_GATEWAY = 502
    }
}
