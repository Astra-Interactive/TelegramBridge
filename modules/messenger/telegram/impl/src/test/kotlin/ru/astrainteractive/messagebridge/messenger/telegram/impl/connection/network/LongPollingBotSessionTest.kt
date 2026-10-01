@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.BOT_USER_NAME
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.fake.FakeBackOff
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.fake.RecordingUpdateConsumer
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.model.TelegramConnectionSettings
import ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping.TelegramFailureMapper
import ru.astrainteractive.messagebridge.messenger.telegram.impl.fake.FakeBotApiServer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class LongPollingBotSessionTest {
    private val server = FakeBotApiServer()
    private val configuration = configurationOf(apiUrl = server.apiUrl)
    private val failureMapper = TelegramFailureMapper(configFlow = MutableStateFlow(configuration))
    private val connection = TelegramConnectionFactory(dns = Dns.SYSTEM)
        .create(TelegramConnectionSettings.of(configuration.tgConfig)) as TelegramConnection.Ready
    private val consumer = RecordingUpdateConsumer()
    private val states: MutableList<TelegramConnectionState> = CopyOnWriteArrayList()
    private val session = LongPollingBotSession(
        connection = connection,
        updateConsumer = consumer,
        failureMapper = failureMapper,
        backOffFactory = { FakeBackOff(interval = 10.milliseconds) },
        timeSource = TimeSource.Monotonic,
        logger = RecordingLogger()
    )

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = TimeSource.Monotonic.markNow() + TIMEOUT
        while (!condition()) {
            check(deadline.hasNotPassedNow()) { "The condition is not met in $TIMEOUT" }
            Thread.sleep(POLL_INTERVAL.inWholeMilliseconds)
        }
    }

    @AfterTest
    fun shutDown() {
        connection.close()
        server.close()
    }

    @Test
    fun GIVEN_working_token_WHEN_bot_is_fetched_THEN_its_username_comes_back() = runTest {
        server.answer(GetMe.PATH, code = 200, body = GET_ME_BODY)

        assertEquals(BOT_USER_NAME, session.fetchBotUserName().getOrThrow())
    }

    @Test
    fun GIVEN_revoked_token_WHEN_bot_is_fetched_THEN_failure_tells_the_token_is_invalid() = runTest {
        server.answer(GetMe.PATH, code = 401, body = UNAUTHORIZED_BODY)

        val error = session.fetchBotUserName().exceptionOrNull()

        assertEquals(TelegramFailure.InvalidToken, error?.let(failureMapper::map))
    }

    @Test
    fun GIVEN_new_message_WHEN_polling_THEN_it_reaches_the_consumer_and_bot_is_connected() = runTest {
        server.answer(DeleteWebhook.PATH, code = 200, body = WEBHOOK_DELETED_BODY)
        server.answer(GetUpdates.PATH, code = 200, body = UPDATES_BODY, delay = 20.milliseconds)

        val polling = session.startPolling("@$BOT_USER_NAME", onState = { state -> states += state }).getOrThrow()
        awaitUntil { consumer.updates.isNotEmpty() && states.isNotEmpty() }
        polling.close()

        assertEquals("hi", consumer.updates.first().message.text)
        assertEquals(TelegramConnectionState.Connected("@$BOT_USER_NAME"), states.first())
    }

    @Test
    fun GIVEN_polling_WHEN_it_is_closed_THEN_telegram_is_not_asked_anymore() = runTest {
        server.answer(DeleteWebhook.PATH, code = 200, body = WEBHOOK_DELETED_BODY)
        server.answer(GetUpdates.PATH, code = 200, body = """{"ok":true,"result":[]}""", delay = 20.milliseconds)
        val polling = session.startPolling("@$BOT_USER_NAME", onState = { state -> states += state }).getOrThrow()
        awaitUntil { states.isNotEmpty() }

        polling.close()
        Thread.sleep(QUIET_PERIOD.inWholeMilliseconds)
        val requestsAfterClose = server.requestedMethods.size
        Thread.sleep(QUIET_PERIOD.inWholeMilliseconds)

        assertEquals(requestsAfterClose, server.requestedMethods.size)
    }

    @Test
    fun GIVEN_telegram_refuses_the_token_WHEN_polling_starts_THEN_it_fails_without_polling() = runTest {
        server.answer(DeleteWebhook.PATH, code = 401, body = UNAUTHORIZED_BODY)

        val error = session.startPolling("@$BOT_USER_NAME", onState = { state -> states += state }).exceptionOrNull()
        Thread.sleep(QUIET_PERIOD.inWholeMilliseconds)

        assertEquals(TelegramFailure.InvalidToken, error?.let(failureMapper::map))
        assertTrue(GetUpdates.PATH !in server.requestedMethods)
    }

    private companion object {
        val TIMEOUT = 10.seconds
        val POLL_INTERVAL = 10.milliseconds
        val QUIET_PERIOD = 200.milliseconds
        const val UNAUTHORIZED_BODY = """{"ok":false,"error_code":401,"description":"Unauthorized"}"""
        const val WEBHOOK_DELETED_BODY = """{"ok":true,"result":true}"""
        const val GET_ME_BODY =
            """{"ok":true,"result":{"id":1,"is_bot":true,"first_name":"Bridge","username":"$BOT_USER_NAME"}}"""
        const val UPDATES_BODY = """{"ok":true,"result":[{"update_id":1,"message":{"message_id":5,""" +
            """"date":1700000000,"chat":{"id":-100,"type":"supergroup"},"text":"hi"}}]}"""
    }
}
