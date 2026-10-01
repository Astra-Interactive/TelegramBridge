@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.relay.internal

import kotlinx.coroutines.flow.MutableStateFlow
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeClock
import ru.astrainteractive.messagebridge.messenger.telegram.relay.model.MessageRelevance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TelegramMessageRelevanceMapperTest {
    private val clock = FakeClock(now = NOW)

    private fun mapperOf(topicId: String): TelegramMessageRelevanceMapper {
        val configuration = PluginConfiguration(
            tgConfig = PluginConfiguration.TelegramConfig(chatID = "$CHAT_ID", topicID = topicId)
        )
        val configFlow = MutableStateFlow(configuration)
        return TelegramMessageRelevanceMapper(configFlow = configFlow, clock = clock)
    }

    private fun updateOf(
        chatId: Long = CHAT_ID,
        threadId: Int? = null,
        isTopicMessage: Boolean = false,
        replyToMessageId: Int? = null,
        date: Instant? = NOW
    ): Update {
        val message = Message().apply {
            chat = Chat(chatId, "supergroup")
            this.date = date?.epochSeconds?.toInt()
            messageThreadId = threadId
            setIsTopicMessage(isTopicMessage)
            replyToMessage = replyToMessageId?.let { id -> Message().apply { messageId = id } }
        }
        return Update().apply { this.message = message }
    }

    @Test
    fun GIVEN_group_without_topics_WHEN_message_is_sent_THEN_it_is_relevant() {
        assertEquals(MessageRelevance.Relevant, mapperOf(topicId = "").map(updateOf()))
    }

    @Test
    fun GIVEN_group_without_topics_WHEN_message_replies_to_another_THEN_it_is_relevant() {
        val reply = updateOf(threadId = 17, replyToMessageId = 17)

        assertEquals(MessageRelevance.Relevant, mapperOf(topicId = "").map(reply))
    }

    @Test
    fun GIVEN_no_topic_configured_WHEN_message_is_in_a_forum_topic_THEN_it_is_wrong_topic() {
        val topicMessage = updateOf(threadId = 42, isTopicMessage = true)

        assertEquals(MessageRelevance.WrongTopic, mapperOf(topicId = "").map(topicMessage))
    }

    @Test
    fun GIVEN_topic_configured_WHEN_message_is_in_that_topic_THEN_it_is_relevant() {
        val topicMessage = updateOf(threadId = 42, isTopicMessage = true)

        assertEquals(MessageRelevance.Relevant, mapperOf(topicId = "42").map(topicMessage))
    }

    @Test
    fun GIVEN_topic_configured_WHEN_message_is_in_another_topic_THEN_it_is_wrong_topic() {
        val topicMessage = updateOf(threadId = 7, isTopicMessage = true)

        assertEquals(MessageRelevance.WrongTopic, mapperOf(topicId = "42").map(topicMessage))
    }

    @Test
    fun GIVEN_reply_thread_configured_WHEN_message_replies_to_its_root_THEN_it_is_relevant() {
        val reply = updateOf(replyToMessageId = 42)

        assertEquals(MessageRelevance.Relevant, mapperOf(topicId = "42").map(reply))
    }

    @Test
    fun GIVEN_another_chat_WHEN_message_is_sent_THEN_it_is_wrong_chat() {
        assertEquals(MessageRelevance.WrongChat, mapperOf(topicId = "").map(updateOf(chatId = 1L)))
    }

    @Test
    fun GIVEN_old_message_WHEN_it_arrives_THEN_it_is_too_old() {
        val oldMessage = updateOf(date = NOW - OLD_MESSAGE_AGE)

        assertEquals(MessageRelevance.TooOld, mapperOf(topicId = "").map(oldMessage))
    }

    @Test
    fun GIVEN_message_sent_while_bot_was_catching_up_WHEN_it_arrives_THEN_it_is_still_relevant() {
        val recentMessage = updateOf(date = NOW - RECENT_MESSAGE_AGE)

        assertEquals(MessageRelevance.Relevant, mapperOf(topicId = "").map(recentMessage))
    }

    @Test
    fun GIVEN_message_without_date_WHEN_it_arrives_THEN_it_has_no_date() {
        assertEquals(MessageRelevance.NoDate, mapperOf(topicId = "").map(updateOf(date = null)))
    }

    @Test
    fun GIVEN_update_without_message_WHEN_it_arrives_THEN_it_is_not_from_the_chat() {
        assertEquals(MessageRelevance.WrongChat, mapperOf(topicId = "").map(Update()))
    }

    private companion object {
        const val CHAT_ID = -1001234567890L
        val NOW: Instant = Instant.fromEpochSeconds(1_700_000_000)
        val OLD_MESSAGE_AGE = 60.seconds
        val RECENT_MESSAGE_AGE = 10.seconds
    }
}
