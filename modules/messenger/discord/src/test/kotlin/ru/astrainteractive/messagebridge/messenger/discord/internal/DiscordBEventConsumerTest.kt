@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.fake.FakeBEventReceiver
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.fake.FakeClock
import ru.astrainteractive.messagebridge.messenger.discord.fake.FakeWebhookClient
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DiscordBEventConsumerTest {
    private val sentMessages = mutableListOf<String>()
    private val webhookClient = FakeWebhookClient()
    private val sentMessage: Message = jdaFake(emptyMap())
    private var sendAnswer = JdaAnswer { args ->
        args.first()
            ?.tryCast<Consumer<Message>>()
            ?.accept(sentMessage)
    }
    private val messageAction: MessageCreateAction = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args -> sendAnswer.answer(args) }
        )
    )
    private var topicEdits = 0
    private var topicManager = JdaAnswer { _ -> throw IllegalStateException("Missing permission MANAGE_CHANNEL") }
    private val textChannel: TextChannel = jdaFake(
        mapOf(
            "sendMessage" to JdaAnswer { args ->
                sentMessages += args.first().toString()
                messageAction
            },
            "getManager" to JdaAnswer { args ->
                topicEdits += 1
                topicManager.answer(args)
            },
            "toString" to "#bridge"
        )
    )
    private val ready = DiscordChannel.Ready(textChannel = textChannel, webhookClient = webhookClient)
    private val steveMessage = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello"
    )
    private val translatedServerTexts = PluginTranslation(
        server = PluginTranslation.Server(
            discordStarted = LocalizedText.shared("Server is up"),
            discordStopped = LocalizedText.shared("Server is down")
        )
    )

    private fun topicManagerHeldByRateLimit(): TextChannelManager {
        lateinit var manager: TextChannelManager
        manager = jdaFake(
            mapOf(
                "setTopic" to JdaAnswer { _ -> manager },
                "timeout" to JdaAnswer { _ -> manager },
                "queue" to null
            )
        )
        return manager
    }

    private fun consumer(state: DiscordChannel): DiscordBEventConsumer = consumer(MutableStateFlow(state))

    private fun consumer(
        discordChannel: Flow<DiscordChannel>,
        translation: PluginTranslation = PluginTranslation()
    ): DiscordBEventConsumer {
        val translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
        return DiscordBEventConsumer(
            discordChannel = discordChannel,
            topicUpdater = DiscordTopicUpdater(
                platformServer = jdaFake(emptyMap()),
                clock = FakeClock(Instant.fromEpochSeconds(0)),
                translationKrate = translationKrate
            ),
            embedMapper = DiscordEmbedMapper(translationKrate),
            memberResolver = DiscordMemberResolver(DiscordAuthorResolver { _ -> null }),
            webhookMessageMapper = DiscordWebhookMessageMapper(),
            translationKrate = translationKrate,
            bEventReceiver = FakeBEventReceiver(emptyFlow())
        )
    }

    @Test
    fun GIVEN_discord_not_configured_WHEN_server_closed_is_consumed_THEN_it_returns_at_once() = runTest {
        consumer(DiscordChannel.Disabled).consume(ServerClosedBEvent)

        assertEquals(Duration.ZERO, currentTime.milliseconds)
        assertTrue(sentMessages.isEmpty())
    }

    @Test
    fun GIVEN_discord_failed_WHEN_chat_message_is_consumed_THEN_it_is_skipped_at_once() = runTest {
        consumer(DiscordChannel.Failed).consume(steveMessage)

        assertEquals(Duration.ZERO, currentTime.milliseconds)
        assertTrue(webhookClient.sent.isEmpty())
    }

    @Test
    fun GIVEN_discord_connecting_WHEN_event_is_consumed_THEN_it_is_skipped_after_thirty_seconds() = runTest {
        consumer(DiscordChannel.Connecting).consume(ServerClosedBEvent)

        assertEquals(CONNECTING_TIMEOUT, currentTime.milliseconds)
        assertTrue(sentMessages.isEmpty())
    }

    @Test
    fun GIVEN_discord_connecting_WHEN_it_gets_ready_within_the_wait_THEN_the_event_is_sent() = runTest {
        val state = MutableStateFlow<DiscordChannel>(DiscordChannel.Connecting)
        val delivery = launch { consumer(state).consume(ServerClosedBEvent) }
        advanceTimeBy(CONNECTING_TIMEOUT / 2)

        state.value = ready
        runCurrent()

        assertTrue(delivery.isCompleted)
        assertEquals(listOf(SERVER_CLOSED_MESSAGE), sentMessages)
    }

    @Test
    fun GIVEN_ready_discord_WHEN_server_closed_is_consumed_THEN_stop_message_is_sent_without_touching_the_topic() =
        runTest {
            consumer(ready).consume(ServerClosedBEvent)

            assertEquals(listOf(SERVER_CLOSED_MESSAGE), sentMessages)
            assertEquals(0, topicEdits)
        }

    @Test
    fun GIVEN_ready_discord_WHEN_minecraft_chat_message_is_consumed_THEN_webhook_sends_it_as_the_player() = runTest {
        consumer(ready).consume(steveMessage)

        val message = webhookClient.sent.single()
        assertEquals("[MC] Steve", message.username)
        assertEquals("hello", message.content)
        assertTrue(sentMessages.isEmpty())
    }

    @Test
    fun GIVEN_webhook_that_fails_WHEN_chat_message_is_consumed_THEN_consume_returns() = runTest {
        webhookClient.failure = IllegalStateException("429: You are being rate limited")

        consumer(ready).consume(steveMessage)

        assertEquals(1, webhookClient.sent.size)
    }

    @Test
    fun GIVEN_jda_cancels_the_send_WHEN_server_closed_is_consumed_THEN_consume_returns_and_the_caller_stays_active() =
        runTest {
            sendAnswer = JdaAnswer { args ->
                args[1]
                    ?.tryCast<Consumer<Throwable>>()
                    ?.accept(CancellationException("RestAction has been cancelled"))
            }

            consumer(ready).consume(ServerClosedBEvent)

            assertTrue(isActive)
        }

    @Test
    fun GIVEN_chat_message_of_only_at_signs_WHEN_consumed_THEN_consume_returns_and_nothing_is_sent() = runTest {
        consumer(ready).consume(steveMessage.copy(text = "@@"))

        assertTrue(webhookClient.sent.isEmpty())
    }

    @Test
    fun GIVEN_discord_connecting_WHEN_consumed_within_the_shutdown_timeout_THEN_it_gives_up_at_that_timeout() =
        runTest {
            val consumed = withTimeoutOrNull(SHUTDOWN_TIMEOUT) {
                consumer(DiscordChannel.Connecting).consume(ServerClosedBEvent)
            }

            assertNull(consumed)
            assertEquals(SHUTDOWN_TIMEOUT, currentTime.milliseconds)
        }

    @Test
    fun GIVEN_discord_that_never_answers_WHEN_consumed_within_the_shutdown_timeout_THEN_it_gives_up_at_that_timeout() =
        runTest {
            sendAnswer = JdaAnswer { _ -> null }

            val consumed = withTimeoutOrNull(SHUTDOWN_TIMEOUT) { consumer(ready).consume(ServerClosedBEvent) }

            assertNull(consumed)
            assertEquals(SHUTDOWN_TIMEOUT, currentTime.milliseconds)
        }

    @Test
    fun GIVEN_ready_discord_WHEN_message_from_discord_is_consumed_THEN_it_is_not_sent_back() = runTest {
        val discordMessage = Text.Discord(author = "Stevie", text = "hi", authorId = 1L, reply = null)

        consumer(ready).consume(discordMessage)

        assertTrue(sentMessages.isEmpty())
        assertTrue(webhookClient.sent.isEmpty())
    }

    @Test
    fun GIVEN_bot_that_cannot_edit_the_topic_WHEN_server_open_is_consumed_THEN_the_start_message_is_still_sent() =
        runTest {
            consumer(ready).consume(ServerOpenBEvent)

            assertEquals(listOf(SERVER_OPEN_MESSAGE), sentMessages)
        }

    @Test
    fun GIVEN_topic_edit_held_by_a_rate_limit_WHEN_server_open_is_consumed_THEN_the_start_message_is_sent_at_once() =
        runTest {
            val manager = topicManagerHeldByRateLimit()
            topicManager = JdaAnswer { _ -> manager }

            consumer(ready).consume(ServerOpenBEvent)

            assertEquals(listOf(SERVER_OPEN_MESSAGE), sentMessages)
            assertEquals(Duration.ZERO, currentTime.milliseconds)
        }

    @Test
    fun GIVEN_translated_server_texts_WHEN_server_open_is_consumed_THEN_the_translated_start_message_is_sent() =
        runTest {
            consumer(MutableStateFlow(ready), translatedServerTexts).consume(ServerOpenBEvent)

            assertEquals(listOf("Server is up"), sentMessages)
        }

    @Test
    fun GIVEN_translated_server_texts_WHEN_server_closed_is_consumed_THEN_the_translated_stop_message_is_sent() =
        runTest {
            consumer(MutableStateFlow(ready), translatedServerTexts).consume(ServerClosedBEvent)

            assertEquals(listOf("Server is down"), sentMessages)
        }

    private companion object {
        const val SERVER_OPEN_MESSAGE = "✅ **The server has started**"
        const val SERVER_CLOSED_MESSAGE = "🛑 **The server has stopped**"
        val CONNECTING_TIMEOUT = 30.seconds
        val SHUTDOWN_TIMEOUT = 5.seconds
    }
}
