@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.fake.FakeBEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.BEvent.Text
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TelegramBEventConsumerTest {
    private val requestedMethods = mutableListOf<String>()
    private val relayedMessageCache = TelegramRelayedMessageCache(capacity = 10)
    private val configured = PluginConfiguration.TelegramConfig(token = TOKEN, chatID = CHAT_ID)

    private fun respond(chain: Interceptor.Chain): Response {
        val request = chain.request()
        requestedMethods += request.url.pathSegments
            .last()
            .lowercase()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"ok":true,"result":$SENT_MESSAGE_JSON}""".toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun consumer(
        tgConfig: PluginConfiguration.TelegramConfig,
        interceptor: Interceptor = Interceptor { chain -> respond(chain) }
    ): TelegramBEventConsumer {
        val telegramClient = OkHttpTelegramClient(
            OkHttpClient.Builder().addInterceptor(interceptor).build(),
            TOKEN
        )
        return TelegramBEventConsumer(
            configKrate = DefaultMutableKrate(
                factory = { PluginConfiguration(tgConfig = tgConfig) },
                loader = { null }
            ).asCachedKrate(),
            translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
                .asCachedKrate(),
            messageSender = TelegramMessageSender(telegramClientFlow = flowOf(telegramClient)),
            relayedMessageCache = relayedMessageCache,
            bEventReceiver = FakeBEventReceiver(emptyFlow())
        )
    }

    @Test
    fun GIVEN_blank_token_WHEN_server_closed_is_consumed_THEN_telegram_is_not_called() = runTest {
        consumer(PluginConfiguration.TelegramConfig(token = "", chatID = CHAT_ID)).consume(ServerClosedBEvent)

        assertTrue(requestedMethods.isEmpty())
    }

    @Test
    fun GIVEN_blank_chat_id_WHEN_server_closed_is_consumed_THEN_telegram_is_not_called() = runTest {
        consumer(PluginConfiguration.TelegramConfig(token = TOKEN, chatID = " ")).consume(ServerClosedBEvent)

        assertTrue(requestedMethods.isEmpty())
    }

    @Test
    fun GIVEN_configured_telegram_WHEN_server_closed_is_consumed_THEN_the_message_is_sent() = runTest {
        consumer(configured).consume(ServerClosedBEvent)

        assertEquals(listOf("sendmessage"), requestedMethods)
    }

    @Test
    fun GIVEN_minecraft_chat_message_WHEN_it_is_sent_THEN_it_is_remembered_for_replies() = runTest {
        val steveMessage = Text.Minecraft(
            author = "Steve",
            uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
            text = "hello"
        )

        consumer(configured).consume(steveMessage)

        assertEquals<BEvent?>(steveMessage, relayedMessageCache.find(SENT_CHAT_ID, SENT_MESSAGE_ID))
    }

    @Test
    fun GIVEN_telegram_that_never_answers_WHEN_server_closed_is_consumed_THEN_it_gives_up_at_the_timeout() =
        runTest {
            val answer = CountDownLatch(1)
            val consumer = consumer(
                tgConfig = configured,
                interceptor = Interceptor { chain ->
                    answer.await()
                    respond(chain)
                }
            )

            val consumed = withTimeoutOrNull(SHUTDOWN_TIMEOUT) { consumer.consume(ServerClosedBEvent) }
            answer.countDown()

            assertNull(consumed)
            assertEquals(SHUTDOWN_TIMEOUT, currentTime.milliseconds)
        }

    private companion object {
        const val TOKEN = "123:token"
        const val CHAT_ID = "-1001"
        const val SENT_CHAT_ID = -1001L
        const val SENT_MESSAGE_ID = 101
        const val SENT_MESSAGE_JSON = """{"message_id":101,"date":1700000000,"chat":{"id":-1001,"type":"supergroup"}}"""
        val SHUTDOWN_TIMEOUT = 5.seconds
    }
}
