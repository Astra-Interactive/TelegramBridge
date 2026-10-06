@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.telegram.internal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
import okio.Buffer
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventReceiver
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TelegramBEventConsumerTest {
    private val objectMapper = ObjectMapper()
    private val requestedMethods = mutableListOf<String>()
    private val requestBodies = mutableListOf<JsonNode>()
    private val configured = PluginConfiguration.TelegramConfig(token = TOKEN, chatID = CHAT_ID)
    private val relayedMessageCache = TelegramRelayedMessageCache(configKrate = configKrate(configured))
    private val steveMessage = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello",
        ref = MessageRef.Minecraft(messageId = "mc-1")
    )
    private val alexReply = Text.Discord(
        author = "Alex",
        text = "yes",
        authorId = ALEX_ID,
        reply = Text.Reply(
            author = "steve_tg",
            authorId = STEVE_ID,
            text = "hi",
            target = MessageRef.Telegram(chatId = SENT_CHAT_ID, messageId = STEVE_MESSAGE_ID)
        ),
        ref = MessageRef.Discord(messageId = 5L)
    )

    private fun configKrate(tgConfig: PluginConfiguration.TelegramConfig): CachedKrate<PluginConfiguration> {
        return DefaultMutableKrate(
            factory = { PluginConfiguration(tgConfig = tgConfig) },
            loader = { null }
        ).asCachedKrate()
    }

    private fun respond(chain: Interceptor.Chain): Response {
        val request = chain.request()
        requestedMethods += request.url.pathSegments
            .last()
            .lowercase()
        val body = Buffer()
            .also { buffer -> request.body?.writeTo(buffer) }
            .readUtf8()
        requestBodies.add(objectMapper.readTree(body))
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
            configKrate = configKrate(tgConfig),
            translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
                .asCachedKrate(),
            messageSender = TelegramMessageSender(telegramClientFlow = flowOf(telegramClient)),
            relayedMessageCache = relayedMessageCache,
            bEventReceiver = FakeBEventReceiver(emptyFlow())
        )
    }

    private fun sentText(): String = requestBodies.single().get("text").asText()

    private fun repliedMessageId(): Int? = requestBodies.single().get("reply_parameters")?.get("message_id")?.asInt()

    private fun blockquotes(): List<Blockquote>? = requestBodies.single().get("entities")
        ?.filter { entity -> entity.get("type").asText() == "blockquote" }
        ?.map { entity -> Blockquote(offset = entity.get("offset").asInt(), length = entity.get("length").asInt()) }

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
    fun GIVEN_configured_topic_WHEN_chat_message_is_consumed_THEN_it_is_sent_into_that_topic() = runTest {
        consumer(configured.copy(topicID = "$TOPIC_ID")).consume(steveMessage)

        assertEquals(TOPIC_ID, requestBodies.single().get("message_thread_id").asInt())
        assertNull(repliedMessageId())
    }

    @Test
    fun GIVEN_minecraft_chat_message_WHEN_it_is_sent_THEN_it_is_remembered_for_replies() = runTest {
        consumer(configured).consume(steveMessage)

        assertEquals<BEvent?>(steveMessage, relayedMessageCache.find(SENT_CHAT_ID, SENT_MESSAGE_ID))
    }

    @Test
    fun GIVEN_discord_reply_to_a_telegram_message_WHEN_consumed_THEN_telegram_replies_to_that_message() = runTest {
        consumer(configured).consume(alexReply)

        assertEquals(STEVE_MESSAGE_ID, repliedMessageId())
        assertTrue(requestBodies.single().get("reply_parameters").get("allow_sending_without_reply").asBoolean())
        assertEquals("[DS] Alex:\nyes", sentText())
        assertNull(blockquotes())
    }

    @Test
    fun GIVEN_discord_reply_to_a_relayed_minecraft_message_WHEN_consumed_THEN_telegram_replies_to_the_copy() = runTest {
        val consumer = consumer(configured)
        consumer.consume(steveMessage)
        requestBodies.clear()

        consumer.consume(alexReply.copy(reply = alexReply.reply?.copy(target = steveMessage.ref)))

        assertEquals(SENT_MESSAGE_ID, repliedMessageId())
        assertEquals("[DS] Alex:\nyes", sentText())
    }

    @Test
    fun GIVEN_discord_reply_to_a_message_telegram_never_saw_WHEN_consumed_THEN_the_replied_text_is_quoted() = runTest {
        val unknown = MessageRef.Minecraft(messageId = "relayed-before-restart")

        consumer(configured).consume(alexReply.copy(reply = alexReply.reply?.copy(target = unknown)))

        assertNull(repliedMessageId())
        assertEquals("[DS] Alex:\nsteve_tg: hi\nyes", sentText())
        assertEquals(listOf(Blockquote(offset = "[DS] Alex:\n".length, length = "steve_tg: hi".length)), blockquotes())
    }

    @Test
    fun GIVEN_discord_reply_to_a_telegram_message_of_another_chat_WHEN_consumed_THEN_the_replied_text_is_quoted() =
        runTest {
            val otherChat = MessageRef.Telegram(chatId = OTHER_CHAT_ID, messageId = STEVE_MESSAGE_ID)

            consumer(configured).consume(alexReply.copy(reply = alexReply.reply?.copy(target = otherChat)))

            assertNull(repliedMessageId())
            assertEquals("[DS] Alex:\nsteve_tg: hi\nyes", sentText())
        }

    @Test
    fun GIVEN_reply_without_a_target_WHEN_consumed_THEN_the_replied_text_is_quoted_and_remembered() = runTest {
        val reply = alexReply.copy(reply = alexReply.reply?.copy(target = null))

        consumer(configured).consume(reply)

        assertEquals("[DS] Alex:\nsteve_tg: hi\nyes", sentText())
        assertEquals<BEvent?>(reply, relayedMessageCache.find(SENT_CHAT_ID, SENT_MESSAGE_ID))
    }

    @Test
    fun GIVEN_replied_text_longer_than_the_preview_length_WHEN_quoted_THEN_it_is_cut() = runTest {
        val longReply = alexReply.copy(reply = alexReply.reply?.copy(text = "hello there", target = null))

        consumer(configured.copy(replyPreviewLength = 5)).consume(longReply)

        assertEquals("[DS] Alex:\nsteve_tg: hello…\nyes", sentText())
        assertEquals(
            listOf(Blockquote(offset = "[DS] Alex:\n".length, length = "steve_tg: hello…".length)),
            blockquotes()
        )
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

    private data class Blockquote(
        val offset: Int,
        val length: Int
    )

    private companion object {
        const val TOKEN = "123:token"
        const val CHAT_ID = "-1001"
        const val SENT_CHAT_ID = -1001L
        const val OTHER_CHAT_ID = -1002L
        const val SENT_MESSAGE_ID = 101
        const val STEVE_MESSAGE_ID = 10
        const val TOPIC_ID = 7
        const val STEVE_ID = 42L
        const val ALEX_ID = 43L
        const val SENT_MESSAGE_JSON = """{"message_id":101,"date":1700000000,"chat":{"id":-1001,"type":"supergroup"}}"""
        val SHUTDOWN_TIMEOUT = 5.seconds
    }
}
