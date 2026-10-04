@file:Suppress("FunctionNaming")
@file:OptIn(ExperimentalCoroutinesApi::class)

package ru.astrainteractive.messagebridge.messenger.discord.messaging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import ru.astrainteractive.messagebridge.messaging.fake.FakeBEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.api.DiscordAuthorResolver
import ru.astrainteractive.messagebridge.messenger.discord.fake.FakeWebhookClient
import ru.astrainteractive.messagebridge.messenger.discord.fake.JdaAnswer
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordEmbedMapper
import ru.astrainteractive.messagebridge.messenger.discord.mapping.DiscordWebhookMessageMapper
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordChannel
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DiscordBEventConsumerTest {
    private val sentMessages = mutableListOf<String>()
    private val webhookClient = FakeWebhookClient()
    private val sentMessage: Message = jdaFake(emptyMap())
    private val messageAction: MessageCreateAction = jdaFake(
        mapOf(
            "queue" to JdaAnswer { args ->
                args.first()
                    ?.tryCast<Consumer<Message>>()
                    ?.accept(sentMessage)
            }
        )
    )
    private val textChannel: TextChannel = jdaFake(
        mapOf(
            "sendMessage" to JdaAnswer { args ->
                sentMessages += args.first().toString()
                messageAction
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

    private fun consumer(state: DiscordChannel): DiscordBEventConsumer = consumer(MutableStateFlow(state))

    private fun consumer(discordChannel: Flow<DiscordChannel>): DiscordBEventConsumer {
        return DiscordBEventConsumer(
            discordChannel = discordChannel,
            topicUpdater = DiscordTopicUpdater(jdaFake(emptyMap())),
            embedMapper = DiscordEmbedMapper(),
            memberResolver = DiscordMemberResolver(DiscordAuthorResolver { _ -> null }),
            webhookMessageMapper = DiscordWebhookMessageMapper(),
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
    fun GIVEN_webhook_that_fails_WHEN_chat_message_is_consumed_THEN_the_failure_reaches_the_caller() = runTest {
        webhookClient.failure = IllegalStateException("429: You are being rate limited")

        val t = assertFailsWith<IllegalStateException> { consumer(ready).consume(steveMessage) }

        assertEquals("429: You are being rate limited", t.message)
    }

    @Test
    fun GIVEN_ready_discord_WHEN_message_from_discord_is_consumed_THEN_it_is_not_sent_back() = runTest {
        val discordMessage = Text.Discord(author = "Stevie", text = "hi", authorId = 1L, reply = null)

        consumer(ready).consume(discordMessage)

        assertTrue(sentMessages.isEmpty())
        assertTrue(webhookClient.sent.isEmpty())
    }

    private companion object {
        const val SERVER_CLOSED_MESSAGE = "🛑 **Сервер остановлен**"
        val CONNECTING_TIMEOUT = 30.seconds
    }
}
