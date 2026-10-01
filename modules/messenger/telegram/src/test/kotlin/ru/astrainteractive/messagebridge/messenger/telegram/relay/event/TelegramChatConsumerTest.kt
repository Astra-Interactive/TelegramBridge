@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.relay.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.messenger.api.fake.RecordingEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.command.internal.TelegramCommandHandler
import ru.astrainteractive.messagebridge.messenger.telegram.command.internal.TelegramCommandParser
import ru.astrainteractive.messagebridge.messenger.telegram.fake.BOT_USER_NAME
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeClock
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeConfigKrate
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.messenger.telegram.fake.NOW
import ru.astrainteractive.messagebridge.messenger.telegram.fake.RecordingLogger
import ru.astrainteractive.messagebridge.messenger.telegram.fake.chatOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.userOf
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramBindHandler
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramAuthorMapper
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramMessageRelevanceMapper
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramMessageValidator
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramRelayedMessageCache
import ru.astrainteractive.messagebridge.messenger.telegram.relay.internal.TelegramReplyMapper
import ru.astrainteractive.messagebridge.messenger.telegram.request.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.internal.TelegramMessageSender
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TelegramChatConsumerTest {
    private val translation = PluginTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val configuration = configurationOf(maxMessageLength = 20, displayNameRegex = "[A-Za-z ]+")
    private val configFlow = MutableStateFlow(configuration)
    private val clock = FakeClock(now = NOW)
    private val botApi = FakeTelegramBotApi()
    private val eventChannel = RecordingEventChannel(logger = RecordingLogger())
    private val messageSender = TelegramMessageSender(
        botApi = botApi,
        maxRetries = 0,
        retryDelay = Duration.ZERO,
        logger = RecordingLogger()
    )
    private val commandParser = TelegramCommandParser(botUserName = { BOT_USER_NAME })
    private val commandHandler = TelegramCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = FakeOnlinePlayersProvider(players = listOf("Steve")),
        linkApi = FakeLinkApi(response = LinkResponse.NoCode),
        bindHandler = TelegramBindHandler(
            bindCodes = BindCodes(clock = clock, lifetime = 10.minutes, random = Random(1)),
            botApi = botApi,
            messageSender = messageSender,
            configKrate = FakeConfigKrate(Result.success(configuration)),
            translationKrate = translationKrate,
            logger = RecordingLogger()
        ),
        chatInfoHandler = TelegramChatInfoHandler(
            messageSender = messageSender,
            translationKrate = translationKrate,
            logger = RecordingLogger()
        ),
        translationKrate = translationKrate
    )

    private fun TestScope.consume(update: Update?) {
        val consumer = TelegramChatConsumer(
            scope = this,
            translationKrate = translationKrate,
            relevanceMapper = TelegramMessageRelevanceMapper(configFlow = configFlow, clock = clock),
            validator = TelegramMessageValidator(
                configFlow = configFlow,
                authorMapper = TelegramAuthorMapper(translationKrate = translationKrate)
            ),
            replyMapper = TelegramReplyMapper(
                configFlow = configFlow,
                authorMapper = TelegramAuthorMapper(translationKrate = translationKrate),
                relayedMessageCache = TelegramRelayedMessageCache(capacity = 10)
            ),
            commandParser = commandParser,
            commandHandler = commandHandler,
            messageSender = messageSender,
            eventChannel = eventChannel,
            logger = RecordingLogger()
        )
        consumer.consume(update)
        advanceUntilIdle()
    }

    @Test
    fun GIVEN_message_in_the_chat_WHEN_consumed_THEN_it_goes_to_minecraft() = runTest {
        consume(updateOf(messageOf(text = "hello", from = userOf(id = 9L, userName = "steve_mc"))))

        val relayed = Text.Telegram(author = "steve_mc", text = "hello", authorId = 9L, reply = null)
        assertEquals(listOf(relayed), eventChannel.events.toList())
        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_message_in_another_chat_WHEN_consumed_THEN_it_is_ignored() = runTest {
        consume(updateOf(messageOf(chat = chatOf(id = -100777))))

        assertTrue(eventChannel.events.isEmpty())
        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_message_sent_long_ago_WHEN_consumed_THEN_it_is_ignored() = runTest {
        consume(updateOf(messageOf(date = NOW - 1.hours)))

        assertTrue(eventChannel.events.isEmpty())
    }

    @Test
    fun GIVEN_message_without_date_WHEN_consumed_THEN_it_is_ignored() = runTest {
        consume(updateOf(messageOf(date = null)))

        assertTrue(eventChannel.events.isEmpty())
    }

    @Test
    fun GIVEN_message_in_another_topic_WHEN_consumed_THEN_it_is_ignored() = runTest {
        consume(updateOf(messageOf(threadId = 12, isTopicMessage = true)))

        assertTrue(eventChannel.events.isEmpty())
    }

    @Test
    fun GIVEN_too_long_message_WHEN_consumed_THEN_it_is_deleted_instead_of_relayed() = runTest {
        consume(updateOf(messageOf(text = "a".repeat(21), messageId = 77)))

        assertTrue(eventChannel.events.isEmpty())
        assertEquals(77, botApi.deletedMessages.single().messageId)
    }

    @Test
    fun GIVEN_name_rejected_by_regex_WHEN_consumed_THEN_author_is_told_and_message_deleted() = runTest {
        consume(updateOf(messageOf(from = userOf(firstName = "Стив"), messageId = 77)))

        assertTrue(eventChannel.events.isEmpty())
        val reply = botApi.sentMessages.single()
        assertEquals(translation.chat.illegalDisplayName.toMessengerText(), reply.text)
        assertEquals(77, reply.replyToMessageId)
        assertEquals(77, botApi.deletedMessages.single().messageId)
    }

    @Test
    fun GIVEN_message_without_text_WHEN_consumed_THEN_nothing_is_relayed() = runTest {
        consume(updateOf(messageOf(text = null)))

        assertTrue(eventChannel.events.isEmpty())
        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_message_without_author_WHEN_consumed_THEN_nothing_is_relayed() = runTest {
        consume(updateOf(messageOf(from = null)))

        assertTrue(eventChannel.events.isEmpty())
    }

    @Test
    fun GIVEN_bot_command_in_the_chat_WHEN_consumed_THEN_it_is_answered_instead_of_relayed() = runTest {
        consume(updateOf(messageOf(text = "/vanilla")))

        assertTrue(eventChannel.events.isEmpty())
        val players = translation.onlinePlayers.message(count = 1, players = "Steve").toMessengerText()
        assertEquals(players, botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_bot_command_in_another_chat_WHEN_consumed_THEN_it_is_ignored() = runTest {
        consume(updateOf(messageOf(chat = chatOf(id = -100777), text = "/vanilla")))

        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_setup_command_in_a_chat_not_set_up_WHEN_consumed_THEN_it_is_answered() = runTest {
        consume(updateOf(messageOf(chat = chatOf(id = -100777), text = "/minfo")))

        assertTrue("-100777" in botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_setup_command_for_another_bot_WHEN_consumed_THEN_it_is_ignored() = runTest {
        consume(updateOf(messageOf(chat = chatOf(id = -100777), text = "/minfo@OtherBot")))

        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_empty_update_WHEN_consumed_THEN_nothing_happens() = runTest {
        consume(update = null)
        consume(updateOf(message = null))

        assertTrue(eventChannel.events.isEmpty())
        assertTrue(botApi.requests.isEmpty())
    }
}
