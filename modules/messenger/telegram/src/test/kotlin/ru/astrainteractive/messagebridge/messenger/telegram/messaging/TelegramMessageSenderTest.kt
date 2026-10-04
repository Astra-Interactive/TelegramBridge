@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertNull

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

    private companion object {
        const val TOKEN = "123:token"
        const val CHAT_ID = "-1001"
        const val FORBIDDEN_JSON = """{"ok":false,"error_code":403,"description":"Forbidden: bot was kicked"}"""
    }
}
