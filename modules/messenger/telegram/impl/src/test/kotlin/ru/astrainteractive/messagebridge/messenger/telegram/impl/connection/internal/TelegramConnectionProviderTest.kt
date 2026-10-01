@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.TelegramConnectionFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramConnectionProviderTest {
    private val configFlow = MutableStateFlow(configurationOf(token = "1:first"))
    private val provider = TelegramConnectionProvider(
        configFlow = configFlow,
        connectionFactory = TelegramConnectionFactory(dns = Dns.SYSTEM)
    )

    private fun TestScope.collectConnections(): List<TelegramConnection> {
        val connections = mutableListOf<TelegramConnection>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { provider.connections.toList(connections) }
        return connections
    }

    private fun TelegramConnection.isClosed(): Boolean {
        return assertIs<TelegramConnection.Ready>(this).okHttpClient.dispatcher.executorService.isShutdown
    }

    private fun apply(configuration: PluginConfiguration) {
        configFlow.value = configuration
    }

    @Test
    fun GIVEN_new_token_WHEN_it_is_applied_THEN_old_connection_is_closed_and_new_one_opened() = runTest {
        val connections = collectConnections()

        apply(configurationOf(token = "2:second"))
        runCurrent()

        val tokens = connections.map { connection -> assertIs<TelegramConnection.Ready>(connection).token }
        assertEquals(listOf("1:first", "2:second"), tokens)
        assertTrue(connections.first().isClosed())
        assertFalse(connections.last().isClosed())
    }

    @Test
    fun GIVEN_new_chat_WHEN_it_is_applied_THEN_connection_is_kept() = runTest {
        val connections = collectConnections()

        apply(configurationOf(token = "1:first", chatId = "-100777", maxMessageLength = 10))
        runCurrent()

        assertEquals(1, connections.size)
        assertFalse(connections.single().isClosed())
    }

    @Test
    fun GIVEN_token_is_removed_WHEN_it_is_applied_THEN_telegram_is_disabled() = runTest {
        val connections = collectConnections()

        apply(configurationOf(token = ""))
        runCurrent()

        assertEquals(TelegramConnection.Disabled, connections.last())
        assertTrue(connections.first().isClosed())
    }

    @Test
    fun GIVEN_token_with_spaces_WHEN_it_is_applied_THEN_connection_is_kept() = runTest {
        val connections = collectConnections()

        apply(configurationOf(token = " 1:first "))
        runCurrent()

        assertEquals(1, connections.size)
    }
}
