@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.telegram.event

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.forum.ForumTopicCreated
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.astralibs.server.bridge.PlatformServer
import ru.astrainteractive.astralibs.server.player.KPlayer
import ru.astrainteractive.astralibs.server.player.OnlineKPlayer
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.api.MessageInterceptor
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Interception
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.command.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.command.TelegramCommandMapper
import ru.astrainteractive.messagebridge.messenger.telegram.fake.DirectExecutorService
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramMessageValidatorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramAuthorMapper
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private data class TelegramRequest(
    val method: String,
    val chatId: String,
    val text: String?,
    val replyToMessageId: Int?
)

private class TestDispatchers(dispatcher: CoroutineDispatcher) : KotlinDispatchers {
    override val Main: MainCoroutineDispatcher
        get() = error("TelegramChatConsumer never switches to the main thread")
    override val IO: CoroutineDispatcher = dispatcher
    override val Default: CoroutineDispatcher = dispatcher
    override val Unconfined: CoroutineDispatcher = dispatcher
}

private object EmptyPlatformServer : PlatformServer {
    override fun getOnlinePlayers(): List<OnlineKPlayer> = emptyList()

    override fun findOnlinePlayer(uuid: UUID): OnlineKPlayer? = null

    override fun findOfflinePlayer(uuid: UUID): KPlayer? = null

    override fun findOnlinePlayer(name: String): OnlineKPlayer? = null

    override fun findOfflinePlayer(name: String): KPlayer? = null
}

private class FixedClock(private val now: Instant) : Clock {
    override fun now(): Instant = now
}

class TelegramChatConsumerTest {
    private val config = PluginConfiguration(
        tgConfig = PluginConfiguration.TelegramConfig(
            chatID = "$CHAT_ID",
            topicID = "$TOPIC_ID",
            displayNameRegex = "[A-Za-z ]+"
        )
    )
    private val configKrate = DefaultMutableKrate(factory = { config }, loader = { null }).asCachedKrate()
    private val translation = PluginTranslation()
    private val translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    private val objectMapper = ObjectMapper()
    private val sentRequests = mutableListOf<TelegramRequest>()
    private val published = FakeBEventConsumer { _ -> }
    private val interceptedTexts = mutableListOf<String?>()
    private val failures = mutableListOf<Throwable>()
    private val steve = User(STEVE_ID, "Steve", false).apply { userName = "steve_tg" }
    private val topicStart = Message().apply {
        messageId = TOPIC_ID
        chat = Chat(CHAT_ID, "supergroup")
        forumTopicCreated = ForumTopicCreated()
    }
    private val messageSender = TelegramMessageSender(
        telegramClientFlow = flowOf(
            OkHttpTelegramClient(
                OkHttpClient.Builder()
                    .dispatcher(Dispatcher(DirectExecutorService))
                    .addInterceptor { chain -> respond(chain) }
                    .build(),
                "123:token"
            )
        )
    )

    private fun respond(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.url.pathSegments
            .last()
            .lowercase()
        val body = Buffer()
            .also { buffer -> request.body?.writeTo(buffer) }
            .readUtf8()
        val json = objectMapper.readTree(body)
        sentRequests += TelegramRequest(
            method = method,
            chatId = json.get("chat_id").asText(),
            text = json.get("text")?.asText(),
            replyToMessageId = json.get("reply_to_message_id")?.asInt()
        )
        val result = if (method == "sendmessage") SENT_MESSAGE_JSON else "true"
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"ok":true,"result":$result}""".toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun recording(interception: Interception) = MessageInterceptor<Update> { update ->
        interceptedTexts += update.message?.text
        interception
    }

    private fun TestScope.chatConsumer(vararg interceptors: MessageInterceptor<Update>): TelegramChatConsumer {
        val ioScope = backgroundScope.coroutineContext.job
            .let(::SupervisorJob)
            .let(backgroundScope.coroutineContext::plus)
            .plus(CoroutineExceptionHandler { _, t -> failures += t })
            .let(::CoroutineScope)
        val authorMapper = TelegramAuthorMapper()
        return TelegramChatConsumer(
            ioScope = ioScope,
            dispatchers = TestDispatchers(StandardTestDispatcher(testScheduler)),
            translationKrate = translationKrate,
            relevanceChecker = TelegramMessageRelevanceMapper(configKrate = configKrate, clock = FixedClock(NOW)),
            validator = TelegramMessageValidatorMapper(configKrate = configKrate, authorMapper = authorMapper),
            replyMapper = TelegramReplyMapper(
                configKrate = configKrate,
                authorMapper = authorMapper,
                relayedMessageCache = TelegramRelayedMessageCache(capacity = 10)
            ),
            commandParser = TelegramCommandMapper(),
            commandHandler = TelegramCommandHandler(
                messageSender = messageSender,
                platformServer = EmptyPlatformServer,
                translationKrate = translationKrate
            ),
            messageSender = messageSender,
            bEventConsumer = published,
            messageInterceptors = interceptors.toList()
        )
    }

    private fun update(
        text: String,
        from: User = steve,
        chatId: Long = CHAT_ID,
        sentAt: Instant = NOW,
        threadId: Int = TOPIC_ID,
        replyTo: Message = topicStart
    ): Update = Update().apply {
        message = Message().apply {
            messageId = MESSAGE_ID
            chat = Chat(chatId, "supergroup")
            this.from = from
            this.text = text
            date = sentAt.epochSeconds.toInt()
            messageThreadId = threadId
            replyToMessage = replyTo
        }
    }

    @Test
    fun GIVEN_interceptor_that_passes_WHEN_message_arrives_THEN_it_is_relayed_with_author_and_reply() = runTest {
        val alex = User(ALEX_ID, "Alex", false).apply { userName = "alex_tg" }
        val alexMessage = Message().apply {
            messageId = 10
            chat = Chat(CHAT_ID, "supergroup")
            from = alex
            text = "hi"
        }
        val consumer = chatConsumer(recording(Interception.Pass))

        consumer.consume(update(text = "hello", replyTo = alexMessage))
        runCurrent()

        val relayed = Text.Telegram(
            author = "steve_tg",
            text = "hello",
            authorId = STEVE_ID,
            reply = Text.Reply(author = "alex_tg", authorId = ALEX_ID, text = "hi")
        )
        assertEquals(listOf<BEvent>(relayed), published.sent)
        assertEquals(listOf<String?>("hello"), interceptedTexts)
    }

    @Test
    fun GIVEN_interceptor_that_consumes_WHEN_message_arrives_THEN_nothing_is_relayed_or_sent() = runTest {
        val consumer = chatConsumer(recording(Interception.Consumed))

        consumer.consume(update(text = "/link 1234"))
        runCurrent()

        assertEquals(emptyList(), published.sent)
        assertEquals(emptyList(), sentRequests)
    }

    @Test
    fun GIVEN_interceptor_that_replies_WHEN_message_arrives_THEN_reply_goes_to_its_topic_and_nothing_is_relayed() =
        runTest {
            val consumer = chatConsumer(recording(Interception.Reply("Your account is linked")))

            consumer.consume(update(text = "/link 1234"))
            runCurrent()

            val answer = TelegramRequest(
                method = "sendmessage",
                chatId = "$CHAT_ID",
                text = "Your account is linked",
                replyToMessageId = TOPIC_ID
            )
            assertEquals(listOf(answer), sentRequests)
            assertEquals(emptyList(), published.sent)
        }

    @Test
    fun GIVEN_message_from_another_chat_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        val consumer = chatConsumer(recording(Interception.Pass))

        consumer.consume(update(text = "/link 1234", chatId = OTHER_CHAT_ID))
        runCurrent()

        assertEquals(emptyList(), interceptedTexts)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_message_older_than_ten_seconds_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        val consumer = chatConsumer(recording(Interception.Pass))

        consumer.consume(update(text = "/link 1234", sentAt = NOW - 11.seconds))
        runCurrent()

        assertEquals(emptyList(), interceptedTexts)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_message_from_another_topic_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        val consumer = chatConsumer(recording(Interception.Pass))

        consumer.consume(update(text = "/link 1234", threadId = OTHER_TOPIC_ID))
        runCurrent()

        assertEquals(emptyList(), interceptedTexts)
        assertEquals(emptyList(), published.sent)
    }

    @Test
    fun GIVEN_author_name_the_regex_rejects_WHEN_message_arrives_THEN_interceptors_never_see_it_and_author_is_told() =
        runTest {
            val consumer = chatConsumer(recording(Interception.Pass))
            val stranger = User(STRANGER_ID, "Стив", false)

            consumer.consume(update(text = "/link 1234", from = stranger))
            runCurrent()

            assertEquals(emptyList(), interceptedTexts)
            assertEquals(listOf("sendmessage", "deletemessage"), sentRequests.map(TelegramRequest::method))
            assertEquals(translation.chat.illegalDisplayName.toMessengerText(), sentRequests.first().text)
        }

    @Test
    fun GIVEN_vanilla_command_WHEN_it_arrives_THEN_interceptors_never_see_it() = runTest {
        val consumer = chatConsumer(recording(Interception.Pass))

        consumer.consume(update(text = "/vanilla"))
        runCurrent()

        assertEquals(emptyList(), interceptedTexts)
        assertEquals(emptyList(), published.sent)
        assertEquals(listOf("sendmessage"), sentRequests.map(TelegramRequest::method))
    }

    @Test
    fun GIVEN_interceptor_that_throws_WHEN_message_arrives_THEN_nothing_is_relayed_and_scope_gets_the_failure() =
        runTest {
            val consumer = chatConsumer(MessageInterceptor { _ -> error("Database is locked") })

            consumer.consume(update(text = "/link 1234"))
            runCurrent()

            assertEquals(emptyList(), published.sent)
            assertEquals(emptyList(), sentRequests)
            assertEquals(listOf<String?>("Database is locked"), failures.map(Throwable::message))
        }

    private companion object {
        const val CHAT_ID = -1001L
        const val OTHER_CHAT_ID = -1002L
        const val TOPIC_ID = 7
        const val OTHER_TOPIC_ID = 8
        const val MESSAGE_ID = 100
        const val STEVE_ID = 42L
        const val ALEX_ID = 43L
        const val STRANGER_ID = 44L
        const val SENT_MESSAGE_JSON = """{"message_id":101,"date":1700000000,"chat":{"id":-1001,"type":"supergroup"}}"""
        val NOW: Instant = Instant.fromEpochSeconds(1_700_000_000)
    }
}
