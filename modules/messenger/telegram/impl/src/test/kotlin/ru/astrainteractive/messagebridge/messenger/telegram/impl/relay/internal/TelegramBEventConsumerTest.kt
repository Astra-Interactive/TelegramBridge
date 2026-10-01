@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeConfigKrate
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.fake.LogLine
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerDeathBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerJoinedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.PlayerLeaveBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping.TelegramFailureTextMapperImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TelegramBEventConsumerTest {
    private val translation = PluginTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val botApi = FakeTelegramBotApi()
    private val logger = RecordingLogger()
    private val relayedMessageCache = TelegramRelayedMessageCache(capacity = 10)
    private val minecraftMessage = Text.Minecraft(author = "Steve", uuid = "uuid", text = "hello")
    private val migratedChatId = -1009876543210L

    private fun consumerOf(
        configuration: PluginConfiguration = configurationOf(),
        configKrate: FakeConfigKrate = FakeConfigKrate(Result.success(configuration))
    ) = TelegramBEventConsumer(
        configFlow = MutableStateFlow(configuration),
        configKrate = configKrate,
        translationKrate = translationKrate,
        botApi = botApi,
        failureTextMapper = TelegramFailureTextMapperImpl(translationKrate = translationKrate),
        relayedMessageCache = relayedMessageCache,
        logger = logger
    )

    private fun failWith(failure: TelegramFailure) {
        botApi.answer = { _ -> TelegramRequestResult.Failed(failure) }
    }

    private fun migrateOnce() {
        botApi.answer = { method ->
            val sendMessage = method as SendMessage
            if (sendMessage.chatId == "$CHAT_ID") {
                TelegramRequestResult.Failed(TelegramFailure.ChatMigrated(migratedChatId))
            } else {
                FakeTelegramBotApi.delivered(method)
            }
        }
    }

    @Test
    fun GIVEN_minecraft_message_WHEN_consumed_THEN_it_is_sent_to_the_chat() = runTest {
        consumerOf().consume(minecraftMessage)

        val sent = botApi.sentMessages.single()
        assertEquals("$CHAT_ID", sent.chatId)
        val expected = translation.chat.toTelegram("Steve", "hello", minecraftMessage.from.short)
        assertEquals(expected.toMessengerText(), sent.text)
        assertNull(sent.replyToMessageId)
    }

    @Test
    fun GIVEN_bound_topic_WHEN_message_is_consumed_THEN_it_goes_into_the_topic() = runTest {
        consumerOf(configurationOf(topicId = "12")).consume(minecraftMessage)

        assertEquals(12, botApi.sentMessages.single().replyToMessageId)
    }

    @Test
    fun GIVEN_message_from_telegram_WHEN_consumed_THEN_it_is_not_echoed_back() = runTest {
        consumerOf().consume(Text.Telegram(author = "Steve", text = "hello", authorId = 7L, reply = null))

        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_server_events_WHEN_consumed_THEN_each_is_announced_with_its_text() = runTest {
        val consumer = consumerOf()
        val player = translation.player

        consumer.consume(PlayerJoinedBEvent(name = "Steve", uuid = "uuid", hasPlayedBefore = true))
        consumer.consume(PlayerJoinedBEvent(name = "Alex", uuid = "uuid", hasPlayedBefore = false))
        consumer.consume(PlayerLeaveBEvent(name = "Steve", uuid = "uuid"))
        consumer.consume(PlayerDeathBEvent(name = "Steve", uuid = "uuid", cause = null))
        consumer.consume(ServerOpenBEvent)
        consumer.consume(ServerClosedBEvent)

        val expected = listOf(
            player.joined("Steve"),
            player.joinedFirstTime("Alex"),
            player.left("Steve"),
            player.died("Steve", cause = null),
            translation.server.started,
            translation.server.stopped
        ).map { text -> text.toMessengerText() }
        assertEquals(expected, botApi.sentMessages.map { sendMessage -> sendMessage.text })
    }

    @Test
    fun GIVEN_no_token_WHEN_message_is_consumed_THEN_nothing_is_sent_nor_reported() = runTest {
        val consumer = consumerOf(configurationOf(token = "", chatId = ""))

        consumer.consume(minecraftMessage)

        assertTrue(botApi.requests.isEmpty())
        assertNull(consumer.deliveryError.value)
    }

    @Test
    fun GIVEN_no_chat_WHEN_messages_are_consumed_THEN_admin_is_told_once_to_bind_one() = runTest {
        val consumer = consumerOf(configurationOf(chatId = ""))

        consumer.consume(minecraftMessage)
        consumer.consume(minecraftMessage)

        assertTrue(botApi.requests.isEmpty())
        val chatNotSet = translation.telegram.errors.chatNotSet
        assertEquals(chatNotSet.toMessengerText(), consumer.deliveryError.value?.toMessengerText())
        assertEquals(1, logger.messagesOf(LogLine.Level.ERROR).size)
    }

    @Test
    fun GIVEN_same_failure_again_WHEN_messages_are_consumed_THEN_it_is_reported_once() = runTest {
        val consumer = consumerOf()
        failWith(TelegramFailure.BotNotInChat)

        consumer.consume(minecraftMessage)
        consumer.consume(minecraftMessage)

        val reason = translation.telegram.errors.botNotInChat
        val expected = translation.telegram.status.deliveryFailed(reason).toMessengerText()
        assertEquals(listOf(expected), logger.messagesOf(LogLine.Level.ERROR))
        assertEquals(reason.toMessengerText(), consumer.deliveryError.value?.toMessengerText())
    }

    @Test
    fun GIVEN_new_failure_WHEN_messages_are_consumed_THEN_it_replaces_the_old_one() = runTest {
        val consumer = consumerOf()
        failWith(TelegramFailure.BotNotInChat)
        consumer.consume(minecraftMessage)

        failWith(TelegramFailure.NoRights)
        consumer.consume(minecraftMessage)

        assertEquals(2, logger.messagesOf(LogLine.Level.ERROR).size)
        val noRights = translation.telegram.errors.noRights
        assertEquals(noRights.toMessengerText(), consumer.deliveryError.value?.toMessengerText())
    }

    @Test
    fun GIVEN_failed_delivery_WHEN_next_message_is_delivered_THEN_error_is_cleared() = runTest {
        val consumer = consumerOf()
        failWith(TelegramFailure.BotNotInChat)
        consumer.consume(minecraftMessage)

        botApi.answer = { method -> FakeTelegramBotApi.delivered(method) }
        consumer.consume(minecraftMessage)

        assertNull(consumer.deliveryError.value)
        val restored = translation.telegram.status.deliveryRestored
        assertEquals(listOf(restored.toMessengerText()), logger.messagesOf(LogLine.Level.INFO))
    }

    @Test
    fun GIVEN_delivered_messages_WHEN_nothing_failed_THEN_nothing_is_logged() = runTest {
        val consumer = consumerOf()

        consumer.consume(minecraftMessage)

        assertTrue(logger.messagesOf(LogLine.Level.INFO).isEmpty())
    }

    @Test
    fun GIVEN_group_became_supergroup_WHEN_message_is_consumed_THEN_new_id_is_saved_and_message_resent() = runTest {
        val configKrate = FakeConfigKrate(Result.success(configurationOf()))
        val consumer = consumerOf(configKrate = configKrate)
        migrateOnce()

        consumer.consume(minecraftMessage)

        assertEquals("$migratedChatId", configKrate.configuration?.tgConfig?.chatID)
        val chatIds = botApi.sentMessages.map { sendMessage -> sendMessage.chatId }
        assertEquals(listOf("$CHAT_ID", "$migratedChatId"), chatIds)
        assertNull(consumer.deliveryError.value)
        val changed = translation.telegram.errors.chatIdChanged(migratedChatId).toMessengerText()
        assertEquals(listOf(changed), logger.messagesOf(LogLine.Level.WARN))
    }

    @Test
    fun GIVEN_group_became_supergroup_WHEN_config_cannot_be_saved_THEN_admin_is_told_to_set_the_new_id() = runTest {
        val configKrate = FakeConfigKrate(Result.failure(IllegalStateException("broken")))
        val consumer = consumerOf(configKrate = configKrate)
        migrateOnce()

        consumer.consume(minecraftMessage)

        assertEquals(1, botApi.sentMessages.size)
        val migrated = translation.telegram.errors.chatMigrated(migratedChatId).toMessengerText()
        assertEquals(migrated, consumer.deliveryError.value?.toMessengerText())
    }

    @Test
    fun GIVEN_chat_message_WHEN_delivered_THEN_a_reply_to_it_finds_the_message() = runTest {
        consumerOf().consume(minecraftMessage)

        val relayed = relayedMessageCache.find(chatId = CHAT_ID, messageId = FakeTelegramBotApi.DELIVERED_MESSAGE_ID)
        assertEquals(minecraftMessage, relayed)
    }

    @Test
    fun GIVEN_join_event_WHEN_delivered_THEN_a_reply_to_it_finds_nothing() = runTest {
        consumerOf().consume(PlayerJoinedBEvent(name = "Steve", uuid = "uuid", hasPlayedBefore = true))

        assertNull(relayedMessageCache.find(chatId = CHAT_ID, messageId = FakeTelegramBotApi.DELIVERED_MESSAGE_ID))
    }

    @Test
    fun GIVEN_failed_delivery_WHEN_chat_message_is_consumed_THEN_a_reply_finds_nothing() = runTest {
        failWith(TelegramFailure.BotNotInChat)

        consumerOf().consume(minecraftMessage)

        assertNull(relayedMessageCache.find(chatId = CHAT_ID, messageId = FakeTelegramBotApi.DELIVERED_MESSAGE_ID))
    }

    @Test
    fun GIVEN_bot_not_connected_WHEN_message_is_consumed_THEN_nothing_is_reported() = runTest {
        val consumer = consumerOf()
        botApi.answer = { _ -> TelegramRequestResult.NotConnected }

        consumer.consume(minecraftMessage)

        assertNull(consumer.deliveryError.value)
        assertTrue(logger.messagesOf(LogLine.Level.ERROR).isEmpty())
    }
}
