@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TelegramMessageSenderTest {
    private var requestCount = 0

    private fun response(chain: Interceptor.Chain, code: Int, body: String): Response {
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Telegram")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun sender(answer: (Interceptor.Chain) -> Response): TelegramMessageSender {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestCount += 1
                answer.invoke(chain)
            }
            .build()
        return TelegramMessageSender(telegramClientFlow = flowOf(OkHttpTelegramClient(okHttpClient, TOKEN)))
    }

    @Test
    fun GIVEN_telegram_rejects_with_403_WHEN_message_is_sent_THEN_it_is_requested_once_and_nothing_is_returned() =
        runTest {
            val sender = sender { chain -> response(chain, code = 403, body = FORBIDDEN_JSON) }

            val sentMessage = sender.send(CHAT_ID, "hello")

            assertNull(sentMessage)
            assertEquals(1, requestCount)
        }

    @Test
    fun GIVEN_network_failure_WHEN_message_is_sent_THEN_it_is_requested_once_and_nothing_is_returned() = runTest {
        val sender = sender { _ -> throw IOException("Connection reset") }

        val sentMessage = sender.send(CHAT_ID, "hello")

        assertNull(sentMessage)
        assertEquals(1, requestCount)
    }

    @Test
    fun GIVEN_telegram_asks_to_wait_3_seconds_WHEN_message_is_sent_THEN_it_is_sent_again_after_3_seconds() = runTest {
        val sender = sender { chain ->
            if (requestCount == 1) {
                response(chain, code = 429, body = floodWaitJson(seconds = 3))
            } else {
                response(chain, code = 200, body = SENT_JSON)
            }
        }

        val sentMessage = sender.send(CHAT_ID, "hello")

        assertNotNull(sentMessage)
        assertEquals(2, requestCount)
        assertEquals(3.seconds, currentTime.milliseconds)
    }

    @Test
    fun GIVEN_telegram_asks_to_wait_twice_WHEN_message_is_sent_THEN_it_gives_up_after_the_second_request() = runTest {
        val sender = sender { chain -> response(chain, code = 429, body = floodWaitJson(seconds = 3)) }

        val sentMessage = sender.send(CHAT_ID, "hello")

        assertNull(sentMessage)
        assertEquals(2, requestCount)
    }

    @Test
    fun GIVEN_telegram_asks_to_wait_a_minute_WHEN_message_is_sent_THEN_it_gives_up_without_waiting() = runTest {
        val sender = sender { chain -> response(chain, code = 429, body = floodWaitJson(seconds = 60)) }

        val sentMessage = sender.send(CHAT_ID, "hello")

        assertNull(sentMessage)
        assertEquals(1, requestCount)
        assertEquals(Duration.ZERO, currentTime.milliseconds)
    }

    private companion object {
        const val TOKEN = "123:token"
        const val CHAT_ID = "-1001"
        const val FORBIDDEN_JSON = """{"ok":false,"error_code":403,"description":"Forbidden: bot was kicked"}"""
        const val SENT_JSON =
            """{"ok":true,"result":{"message_id":101,"date":1700000000,"chat":{"id":-1001,"type":"supergroup"}}}"""

        fun floodWaitJson(seconds: Int): String {
            return """{"ok":false,"error_code":429,"description":"Too Many Requests: retry after $seconds",""" +
                """"parameters":{"retry_after":$seconds}}"""
        }
    }
}
