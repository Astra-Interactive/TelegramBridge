@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.internal

import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.TextQuote
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.forum.ForumTopicCreated
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.telegram.mapping.TelegramAuthorMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramReplyMapperTest {
    private val config = PluginConfiguration(
        tgConfig = PluginConfiguration.TelegramConfig(chatID = "$CHAT_ID", topicID = "$TOPIC_ID")
    )
    private val relayedMessageCache = TelegramRelayedMessageCache(capacity = 10)
    private val mapper = TelegramReplyMapper(
        configKrate = DefaultMutableKrate(factory = { config }, loader = { null }).asCachedKrate(),
        authorMapper = TelegramAuthorMapper(),
        relayedMessageCache = relayedMessageCache
    )
    private val steve = User(STEVE_ID, "Steve", false).apply { userName = "steve_tg" }
    private val bot = User(BOT_ID, "MessageBridge", true).apply { userName = "MessageBridgeBot" }

    private fun message(
        id: Int,
        from: User?,
        text: String? = null,
        replyTo: Message? = null
    ): Message = Message().apply {
        messageId = id
        chat = Chat(CHAT_ID, "supergroup")
        this.from = from
        this.text = text
        replyToMessage = replyTo
    }

    private fun replyTo(replied: Message): Message {
        val alex = User(ALEX_ID, "Alex", false)
        return message(id = 100, from = alex, text = "hi", replyTo = replied)
    }

    @Test
    fun GIVEN_message_that_replies_to_nothing_WHEN_mapped_THEN_returns_null() = runTest {
        assertNull(mapper.map(message(id = 100, from = steve, text = "hi")))
    }

    @Test
    fun GIVEN_reply_to_user_WHEN_mapped_THEN_reply_names_user_and_text() = runTest {
        val replied = message(id = 10, from = steve, text = "hello")

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "steve_tg", authorId = STEVE_ID, text = "hello"), reply)
    }

    @Test
    fun GIVEN_reply_to_photo_with_caption_WHEN_mapped_THEN_text_is_caption() = runTest {
        val replied = message(id = 10, from = steve).apply { caption = "my base" }

        val reply = mapper.map(replyTo(replied))

        assertEquals("my base", reply?.text)
    }

    @Test
    fun GIVEN_reply_to_media_without_caption_WHEN_mapped_THEN_text_is_empty() = runTest {
        val replied = message(id = 10, from = steve)

        val reply = mapper.map(replyTo(replied))

        assertEquals("", reply?.text)
    }

    @Test
    fun GIVEN_reply_that_quotes_part_of_message_WHEN_mapped_THEN_text_is_quote() = runTest {
        val replied = message(id = 10, from = steve, text = "hello, meet me at spawn")
        val message = replyTo(replied).apply { quote = TextQuote().apply { text = "at spawn" } }

        val reply = mapper.map(message)

        assertEquals(Text.Reply(author = "steve_tg", authorId = STEVE_ID, text = "at spawn"), reply)
    }

    @Test
    fun GIVEN_reply_to_message_bot_relayed_WHEN_mapped_THEN_reply_names_player_who_wrote_it() = runTest {
        val relayed = Text.Minecraft(author = "Steve", uuid = "8667ba71-b85a-4004-af54-457a9734eed7", text = "hello")
        relayedMessageCache.remember(chatId = CHAT_ID, messageId = 10, text = relayed)
        val replied = message(id = 10, from = bot, text = "[MC] Steve:\nhello")

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "Steve", authorId = null, text = "hello"), reply)
    }

    @Test
    fun GIVEN_reply_to_bot_message_relayed_before_restart_WHEN_mapped_THEN_reply_names_bot() = runTest {
        val replied = message(id = 10, from = bot, text = "[MC] Steve:\nhello")

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "MessageBridgeBot", authorId = BOT_ID, text = "[MC] Steve:\nhello"), reply)
    }

    @Test
    fun GIVEN_message_relayed_in_another_chat_WHEN_reply_to_same_id_is_mapped_THEN_reply_names_its_author() = runTest {
        val relayed = Text.Minecraft(author = "Steve", uuid = "8667ba71-b85a-4004-af54-457a9734eed7", text = "hello")
        relayedMessageCache.remember(chatId = OTHER_CHAT_ID, messageId = 10, text = relayed)
        val replied = message(id = 10, from = steve, text = "different")

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "steve_tg", authorId = STEVE_ID, text = "different"), reply)
    }

    @Test
    fun GIVEN_message_in_forum_topic_WHEN_mapped_THEN_topic_start_is_not_a_reply() = runTest {
        val topicStart = message(id = 50, from = steve).apply { forumTopicCreated = ForumTopicCreated() }

        assertNull(mapper.map(replyTo(topicStart)))
    }

    @Test
    fun GIVEN_message_under_configured_topic_message_WHEN_mapped_THEN_topic_message_is_not_a_reply() = runTest {
        val topicMessage = message(id = TOPIC_ID, from = steve, text = "Minecraft chat")

        assertNull(mapper.map(replyTo(topicMessage)))
    }

    @Test
    fun GIVEN_reply_to_message_without_author_WHEN_mapped_THEN_returns_null() = runTest {
        val replied = message(id = 10, from = null, text = "hello")

        assertNull(mapper.map(replyTo(replied)))
    }

    private companion object {
        const val CHAT_ID = -1001L
        const val OTHER_CHAT_ID = -1002L
        const val TOPIC_ID = 7
        const val STEVE_ID = 42L
        const val ALEX_ID = 43L
        const val BOT_ID = 99L
    }
}
