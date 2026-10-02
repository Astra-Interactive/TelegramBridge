@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping

import kotlinx.coroutines.flow.MutableStateFlow
import org.telegram.telegrambots.longpolling.exceptions.TelegramApiErrorResponseException
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TelegramFailureMapperTest {
    private val proxy = PluginConfiguration.Proxy(host = "127.0.0.1", port = 3128)

    private fun mapperOf(
        proxy: PluginConfiguration.Proxy? = null,
        apiUrl: String = ""
    ): TelegramFailureMapper {
        val configuration = PluginConfiguration(
            tgConfig = PluginConfiguration.TelegramConfig(proxy = proxy, apiUrl = apiUrl)
        )
        val configFlow = MutableStateFlow(configuration)
        return TelegramFailureMapper(configFlow = configFlow)
    }

    private fun apiError(response: String): TelegramApiRequestException {
        return assertFailsWith<TelegramApiRequestException> {
            SendMessage("1", "text").deserializeResponse(response)
        }
    }

    private fun apiError(code: Int, description: String, parameters: String? = null): TelegramApiRequestException {
        val parametersField = parameters?.let { json -> ""","parameters":$json""" }.orEmpty()
        return apiError("""{"ok":false,"error_code":$code,"description":"$description"$parametersField}""")
    }

    private fun networkError(cause: Throwable) = TelegramApiException("Unable to execute sendmessage method", cause)

    @Test
    fun GIVEN_unauthorized_WHEN_mapped_THEN_token_is_invalid() {
        assertEquals(TelegramFailure.InvalidToken, mapperOf().map(apiError(401, "Unauthorized")))
    }

    @Test
    fun GIVEN_not_found_WHEN_mapped_THEN_token_is_invalid() {
        assertEquals(TelegramFailure.InvalidToken, mapperOf().map(apiError(404, "Not Found")))
    }

    @Test
    fun GIVEN_chat_not_found_WHEN_mapped_THEN_chat_is_not_found() {
        val t = apiError(400, "Bad Request: chat not found")

        assertEquals(TelegramFailure.ChatNotFound, mapperOf().map(t))
    }

    @Test
    fun GIVEN_group_upgraded_to_supergroup_WHEN_mapped_THEN_new_chat_id_is_given() {
        val t = apiError(
            code = 400,
            description = "Bad Request: group chat was upgraded to a supergroup chat",
            parameters = """{"migrate_to_chat_id":-1001234567890}"""
        )

        assertEquals(TelegramFailure.ChatMigrated(-1001234567890), mapperOf().map(t))
    }

    @Test
    fun GIVEN_topic_errors_WHEN_mapped_THEN_topic_is_not_found() {
        val descriptions = listOf(
            "Bad Request: message thread not found",
            "Bad Request: TOPIC_CLOSED",
            "Bad Request: TOPIC_DELETED",
            "Bad Request: message to be replied not found"
        )

        descriptions.forEach { description ->
            assertEquals(TelegramFailure.TopicNotFound, mapperOf().map(apiError(400, description)), description)
        }
    }

    @Test
    fun GIVEN_bot_removed_from_chat_WHEN_mapped_THEN_bot_is_not_in_chat() {
        val descriptions = listOf(
            "Forbidden: bot was kicked from the supergroup chat",
            "Forbidden: bot is not a member of the supergroup chat",
            "Forbidden: bot was blocked by the user"
        )

        descriptions.forEach { description ->
            assertEquals(TelegramFailure.BotNotInChat, mapperOf().map(apiError(403, description)), description)
        }
    }

    @Test
    fun GIVEN_missing_rights_WHEN_mapped_THEN_bot_has_no_rights() {
        val descriptions = listOf(
            "Bad Request: not enough rights to send text messages to the chat",
            "Bad Request: have no rights to send a message",
            "Bad Request: CHAT_WRITE_FORBIDDEN",
            "Bad Request: message can't be deleted"
        )

        descriptions.forEach { description ->
            assertEquals(TelegramFailure.NoRights, mapperOf().map(apiError(400, description)), description)
        }
    }

    @Test
    fun GIVEN_forbidden_to_write_WHEN_mapped_THEN_rights_win_over_membership() {
        val t = apiError(403, "Forbidden: CHAT_WRITE_FORBIDDEN")

        assertEquals(TelegramFailure.NoRights, mapperOf().map(t))
    }

    @Test
    fun GIVEN_conflict_WHEN_mapped_THEN_token_is_in_use() {
        val t = apiError(409, "Conflict: terminated by other getUpdates request")

        assertEquals(TelegramFailure.TokenInUse, mapperOf().map(t))
    }

    @Test
    fun GIVEN_too_many_requests_WHEN_mapped_THEN_it_is_rate_limited_with_delay() {
        val t = apiError(429, "Too Many Requests: retry after 5", parameters = """{"retry_after":5}""")

        assertEquals(TelegramFailure.RateLimited(retryAfter = 5.seconds), mapperOf().map(t))
    }

    @Test
    fun GIVEN_server_error_WHEN_mapped_THEN_it_is_transient() {
        val failure = mapperOf().map(apiError(502, "Bad Gateway"))

        assertEquals(TelegramFailure.ServerError(502), failure)
        assertTrue(failure.isTransient)
    }

    @Test
    fun GIVEN_other_api_error_WHEN_mapped_THEN_description_is_kept() {
        val t = apiError(400, "Bad Request: message text is empty")

        assertEquals(TelegramFailure.Unknown("Bad Request: message text is empty"), mapperOf().map(t))
    }

    @Test
    fun GIVEN_network_errors_WHEN_mapped_THEN_it_is_a_network_failure() {
        val causes = listOf(
            UnknownHostException("api.telegram.org"),
            ConnectException("Connection refused"),
            SocketTimeoutException("connect timed out"),
            SSLHandshakeException("Remote host terminated the handshake")
        )

        causes.forEach { cause ->
            val failure = mapperOf().map(networkError(cause))

            assertEquals(TelegramFailure.Network(proxy = null, apiUrl = null), failure, "$cause")
            assertTrue(failure.isTransient)
        }
    }

    @Test
    fun GIVEN_proxy_WHEN_network_fails_THEN_failure_names_the_proxy() {
        val failure = mapperOf(proxy = proxy).map(networkError(ConnectException("Connection refused")))

        assertEquals(TelegramFailure.Network(proxy = proxy, apiUrl = null), failure)
    }

    @Test
    fun GIVEN_api_url_WHEN_network_fails_THEN_failure_names_the_server() {
        val failure = mapperOf(apiUrl = " https://tg.example.com ")
            .map(networkError(UnknownHostException("tg.example.com")))

        assertEquals(TelegramFailure.Network(proxy = null, apiUrl = "https://tg.example.com"), failure)
    }

    @Test
    fun GIVEN_deep_cause_WHEN_mapped_THEN_whole_chain_is_searched() {
        val t = ExecutionException(networkError(IOException("wrapper", UnknownHostException("api.telegram.org"))))

        assertEquals(TelegramFailure.Network(proxy = null, apiUrl = null), mapperOf().map(t))
    }

    @Test
    fun GIVEN_rejected_proxy_credentials_WHEN_mapped_THEN_proxy_auth_fails() {
        val t = networkError(IOException("Failed to authenticate with proxy"))

        val failure = mapperOf(proxy = proxy).map(t)

        assertEquals(TelegramFailure.ProxyAuth, failure)
        assertTrue(failure.needsNewSettings)
    }

    @Test
    fun GIVEN_polling_unauthorized_WHEN_mapped_THEN_token_is_invalid() {
        val t = TelegramApiErrorResponseException(401, "Unauthorized")

        assertEquals(TelegramFailure.InvalidToken, mapperOf().map(t))
    }

    @Test
    fun GIVEN_polling_conflict_over_http2_WHEN_reason_is_empty_THEN_token_is_in_use() {
        val t = TelegramApiErrorResponseException(409, "")

        assertEquals(TelegramFailure.TokenInUse, mapperOf().map(t))
    }

    @Test
    fun GIVEN_polling_failed_to_connect_WHEN_mapped_THEN_it_is_a_network_failure() {
        val t = TelegramApiErrorResponseException(SocketTimeoutException("timeout"))

        assertEquals(TelegramFailure.Network(proxy = null, apiUrl = null), mapperOf().map(t))
    }

    @Test
    fun GIVEN_api_url_WHEN_answer_is_not_json_THEN_api_url_is_invalid() {
        val t = apiError("<html><body>Welcome to nginx!</body></html>")

        val failure = mapperOf(apiUrl = "https://example.com").map(t)

        assertEquals(TelegramFailure.InvalidApiUrl, failure)
        assertTrue(failure.needsNewSettings)
    }

    @Test
    fun GIVEN_telegram_url_WHEN_answer_is_not_json_THEN_failure_is_unknown() {
        val failure = mapperOf().map(apiError("<html></html>"))

        assertTrue(failure is TelegramFailure.Unknown, "$failure")
        assertFalse(failure.isTransient)
    }

    @Test
    fun GIVEN_chat_not_found_WHEN_mapped_THEN_it_is_not_retried() {
        val failure = mapperOf().map(apiError(400, "Bad Request: chat not found"))

        assertFalse(failure.isTransient)
        assertFalse(failure.needsNewSettings)
    }

    @Test
    fun GIVEN_empty_chat_id_WHEN_mapped_THEN_chat_is_not_set() {
        val t = apiError(400, "Bad Request: chat_id is empty")

        assertEquals(TelegramFailure.ChatNotSet, mapperOf().map(t))
    }

    @Test
    fun GIVEN_forbidden_without_known_reason_WHEN_mapped_THEN_bot_is_not_in_chat() {
        val t = apiError(403, "Forbidden: something new")

        assertEquals(TelegramFailure.BotNotInChat, mapperOf().map(t))
    }

    @Test
    fun GIVEN_too_many_requests_without_delay_WHEN_mapped_THEN_it_is_rate_limited_without_delay() {
        val t = apiError(429, "Too Many Requests")

        assertEquals(TelegramFailure.RateLimited(retryAfter = null), mapperOf().map(t))
    }

    @Test
    fun GIVEN_polling_server_error_WHEN_mapped_THEN_it_is_a_server_error() {
        val t = TelegramApiErrorResponseException(502, "Bad Gateway")

        assertEquals(TelegramFailure.ServerError(502), mapperOf().map(t))
    }

    @Test
    fun GIVEN_polling_too_many_requests_WHEN_mapped_THEN_it_is_rate_limited_without_delay() {
        val t = TelegramApiErrorResponseException(429, "Too Many Requests")

        assertEquals(TelegramFailure.RateLimited(retryAfter = null), mapperOf().map(t))
    }

    @Test
    fun GIVEN_polling_reason_without_code_WHEN_mapped_THEN_reason_tells_the_failure() {
        assertEquals(TelegramFailure.InvalidToken, mapperOf().map(TelegramApiErrorResponseException("Unauthorized")))
        assertEquals(TelegramFailure.TokenInUse, mapperOf().map(TelegramApiErrorResponseException("Conflict")))
    }

    @Test
    fun GIVEN_polling_other_code_WHEN_mapped_THEN_code_and_reason_are_kept() {
        val t = TelegramApiErrorResponseException(418, "I'm a teapot")

        assertEquals(TelegramFailure.Unknown("418 I'm a teapot"), mapperOf().map(t))
    }

    @Test
    fun GIVEN_proxy_refusing_tunnels_WHEN_mapped_THEN_proxy_auth_fails() {
        val t = networkError(IOException("Too many tunnel connections attempted: 21"))

        assertEquals(TelegramFailure.ProxyAuth, mapperOf(proxy = proxy).map(t))
    }

    @Test
    fun GIVEN_unknown_error_with_cause_WHEN_mapped_THEN_both_messages_are_kept() {
        val t = IllegalStateException("outer", IllegalArgumentException("root"))

        assertEquals(TelegramFailure.Unknown("outer: IllegalArgumentException: root"), mapperOf().map(t))
    }

    @Test
    fun GIVEN_error_without_message_WHEN_mapped_THEN_its_type_is_kept() {
        assertEquals(TelegramFailure.Unknown("IllegalStateException"), mapperOf().map(IllegalStateException()))
    }
}
