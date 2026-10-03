@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.messaging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramBEventConsumerTest {
    private val requestedMethods = mutableListOf<String>()
    private val noEvents = object : BEventReceiver {
        override fun bEvents(scope: CoroutineScope): Flow<BEvent> = emptyFlow()
    }

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

    private fun consumer(tgConfig: PluginConfiguration.TelegramConfig): TelegramBEventConsumer {
        val telegramClient = OkHttpTelegramClient(
            OkHttpClient.Builder().addInterceptor { chain -> respond(chain) }.build(),
            TOKEN
        )
        return TelegramBEventConsumer(
            configKrate = DefaultMutableKrate(
                factory = { PluginConfiguration(tgConfig = tgConfig) },
                loader = { null }
            ).asCachedKrate(),
            translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
                .asCachedKrate(),
            telegramClientFlow = flowOf(telegramClient),
            relayedMessageCache = TelegramRelayedMessageCache(capacity = 10),
            bEventReceiver = noEvents
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
        consumer(PluginConfiguration.TelegramConfig(token = TOKEN, chatID = CHAT_ID)).consume(ServerClosedBEvent)

        assertEquals(listOf("sendmessage"), requestedMethods)
    }

    private companion object {
        const val TOKEN = "123:token"
        const val CHAT_ID = "-1001"
        const val SENT_MESSAGE_JSON = """{"message_id":101,"date":1700000000,"chat":{"id":-1001,"type":"supergroup"}}"""
    }
}
