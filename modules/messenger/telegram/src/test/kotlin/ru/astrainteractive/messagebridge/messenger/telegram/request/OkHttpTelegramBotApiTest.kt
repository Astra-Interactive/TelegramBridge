@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.request

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.messenger.telegram.FakeBotApiServer
import ru.astrainteractive.messagebridge.messenger.telegram.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.connection.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.connection.TelegramConnectionFactory
import ru.astrainteractive.messagebridge.messenger.telegram.connection.TelegramConnectionSettings
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailureMapper
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OkHttpTelegramBotApiTest {
    private val server = FakeBotApiServer()
    private val configuration = configurationOf(apiUrl = server.apiUrl)
    private val failureMapper = TelegramFailureMapper(configFlow = MutableStateFlow(configuration))
    private val connection = TelegramConnectionFactory(dns = Dns.SYSTEM)
        .create(TelegramConnectionSettings.of(configuration.tgConfig)) as TelegramConnection.Ready

    private fun botApiOf(client: OkHttpTelegramClient?) = OkHttpTelegramBotApi(
        telegramClients = flowOf(client),
        failureMapper = failureMapper
    )

    @AfterTest
    fun shutDown() {
        connection.close()
        server.close()
    }

    @Test
    fun GIVEN_no_bot_WHEN_request_is_made_THEN_it_is_not_connected() = runTest {
        val result = botApiOf(client = null).execute(SendMessage("1", "text"))

        assertEquals(TelegramRequestResult.NotConnected, result)
    }

    @Test
    fun GIVEN_no_connection_yet_WHEN_request_is_made_THEN_it_is_not_connected() = runTest {
        val botApi = OkHttpTelegramBotApi(telegramClients = emptyFlow(), failureMapper = failureMapper)

        assertEquals(TelegramRequestResult.NotConnected, botApi.execute(SendMessage("1", "text")))
    }

    @Test
    fun GIVEN_telegram_accepts_the_message_WHEN_it_is_sent_THEN_the_message_comes_back() = runTest {
        server.answer(
            method = SendMessage.PATH,
            code = 200,
            body = """{"ok":true,"result":{"message_id":42,"date":1700000000,"chat":{"id":1,"type":"group"}}}"""
        )

        val result = botApiOf(connection.telegramClient).execute(SendMessage("1", "text"))

        val message: Message = assertIs<TelegramRequestResult.Success<Message>>(result).value
        assertEquals(42, message.messageId)
    }

    @Test
    fun GIVEN_telegram_rejects_the_chat_WHEN_message_is_sent_THEN_failure_tells_why() = runTest {
        server.answer(
            method = SendMessage.PATH,
            code = 400,
            body = """{"ok":false,"error_code":400,"description":"Bad Request: chat not found"}"""
        )

        val result = botApiOf(connection.telegramClient).execute(SendMessage("1", "text"))

        assertEquals(TelegramRequestResult.Failed(TelegramFailure.ChatNotFound), result)
    }
}
