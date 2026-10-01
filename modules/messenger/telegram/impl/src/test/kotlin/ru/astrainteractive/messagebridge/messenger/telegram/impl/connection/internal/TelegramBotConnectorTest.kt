@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import org.telegram.telegrambots.meta.TelegramUrl
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.BOT_USER_NAME
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.api.TelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.fake.FakeBackOff
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.fake.FakeTelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping.TelegramFailureMapper
import java.io.IOException
import java.net.ConnectException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class TelegramBotConnectorTest {
    private val okHttpClient = OkHttpClient()
    private val ready = TelegramConnection.Ready(
        token = TOKEN,
        url = TelegramUrl.DEFAULT_URL,
        okHttpClient = okHttpClient,
        telegramClient = OkHttpTelegramClient(okHttpClient, TOKEN)
    )
    private val backOff = FakeBackOff(interval = RETRY_INTERVAL)
    private val network = TelegramFailure.Network(proxy = null, apiUrl = null)

    private fun sessionOf(
        userNameResults: List<Result<String>> = emptyList(),
        pollingResults: List<Result<Unit>> = emptyList()
    ) = FakeTelegramBotSession(
        userNameResults = userNameResults,
        pollingResults = pollingResults,
        userName = BOT_USER_NAME
    )

    private fun connectorOf(session: TelegramBotSession) = TelegramBotConnector(
        sessionFactory = { _ -> session },
        backOffFactory = { backOff },
        failureMapper = TelegramFailureMapper(configFlow = MutableStateFlow(PluginConfiguration())),
        logger = RecordingLogger()
    )

    private fun TestScope.connect(
        connector: TelegramBotConnector,
        connections: MutableStateFlow<TelegramConnection>
    ) {
        backgroundScope.launch { connector.connect(connections) }
        runCurrent()
    }

    @AfterTest
    fun closeConnection() {
        ready.close()
    }

    @Test
    fun GIVEN_no_token_WHEN_connecting_THEN_bot_is_disabled() = runTest {
        val connector = connectorOf(sessionOf())

        connect(connector, MutableStateFlow(TelegramConnection.Disabled))

        assertEquals(TelegramConnectionState.Disabled, connector.state.value)
        assertNull(connector.botUserName)
    }

    @Test
    fun GIVEN_settings_that_cannot_work_WHEN_connecting_THEN_bot_fails_with_their_reason() = runTest {
        val connector = connectorOf(sessionOf())

        connect(connector, MutableStateFlow(TelegramConnection.Invalid(TelegramFailure.SocksWithPassword)))

        assertEquals(TelegramConnectionState.Failed(TelegramFailure.SocksWithPassword), connector.state.value)
    }

    @Test
    fun GIVEN_working_token_WHEN_connecting_THEN_bot_polls_under_its_name() = runTest {
        val session = sessionOf()
        val connector = connectorOf(session)

        connect(connector, MutableStateFlow(ready))

        assertEquals(TelegramConnectionState.Connected("@$BOT_USER_NAME"), connector.state.value)
        assertEquals(BOT_USER_NAME, connector.botUserName)
        assertEquals(1, session.pollings.size)
        assertEquals(1, backOff.resets.get())
    }

    @Test
    fun GIVEN_revoked_token_WHEN_connecting_THEN_it_is_not_retried_with_the_same_settings() = runTest {
        val unauthorized = TelegramApiErrorResponseException(401, "Unauthorized")
        val session = sessionOf(userNameResults = listOf(Result.failure(unauthorized)))
        val connector = connectorOf(session)

        connect(connector, MutableStateFlow(ready))
        advanceTimeBy(1.hours)

        assertEquals(TelegramConnectionState.Failed(TelegramFailure.InvalidToken), connector.state.value)
        assertEquals(1, session.fetches.get())
        assertNull(connector.botUserName)
    }

    @Test
    fun GIVEN_network_is_down_WHEN_connecting_THEN_it_is_retried_after_the_backoff() = runTest {
        val session = sessionOf(userNameResults = listOf(Result.failure(ConnectException("Connection refused"))))
        val connector = connectorOf(session)

        connect(connector, MutableStateFlow(ready))

        assertEquals(TelegramConnectionState.Failed(network), connector.state.value)
        advanceTimeBy(RETRY_INTERVAL + 1.seconds)
        assertEquals(TelegramConnectionState.Connected("@$BOT_USER_NAME"), connector.state.value)
        assertEquals(2, session.fetches.get())
    }

    @Test
    fun GIVEN_polling_cannot_start_WHEN_connecting_THEN_backoff_grows_until_polling_starts() = runTest {
        val session = sessionOf(
            pollingResults = listOf(Result.failure(IOException("reset")), Result.failure(IOException("reset")))
        )
        val connector = connectorOf(session)

        connect(connector, MutableStateFlow(ready))
        assertEquals(TelegramConnectionState.Failed(network), connector.state.value)
        assertEquals(0, backOff.resets.get())

        advanceTimeBy((RETRY_INTERVAL + 1.seconds) * 2)

        assertEquals(TelegramConnectionState.Connected("@$BOT_USER_NAME"), connector.state.value)
        assertEquals(2, backOff.intervalsGiven.get())
        assertEquals(1, backOff.resets.get())
    }

    @Test
    fun GIVEN_polling_bot_WHEN_polling_reports_a_conflict_THEN_state_follows_it() = runTest {
        val session = sessionOf()
        val connector = connectorOf(session)
        connect(connector, MutableStateFlow(ready))

        session.reportState(TelegramConnectionState.Failed(TelegramFailure.TokenInUse))

        assertEquals(TelegramConnectionState.Failed(TelegramFailure.TokenInUse), connector.state.value)
    }

    @Test
    fun GIVEN_polling_bot_WHEN_token_is_removed_THEN_polling_stops() = runTest {
        val session = sessionOf()
        val connector = connectorOf(session)
        val connections = MutableStateFlow<TelegramConnection>(ready)
        connect(connector, connections)

        connections.value = TelegramConnection.Disabled
        runCurrent()

        assertTrue(session.pollings.single().isClosed)
        assertEquals(TelegramConnectionState.Disabled, connector.state.value)
        assertNull(connector.botUserName)
    }

    @Test
    fun GIVEN_polling_bot_WHEN_connector_is_cancelled_THEN_polling_stops() = runTest {
        val session = sessionOf()
        val connector = connectorOf(session)
        val job = launch { connector.connect(MutableStateFlow(ready)) }
        runCurrent()

        job.cancel()
        runCurrent()

        assertTrue(session.pollings.single().isClosed)
    }

    private companion object {
        const val TOKEN = "123:token"
        val RETRY_INTERVAL = 5.seconds
    }
}
