@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.command

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.chatOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramChatInfoHandlerTest {
    private val botApi = FakeTelegramBotApi()
    private val chatInfo = OnboardingTranslation().telegram.chatInfo
    private val handler = TelegramChatInfoHandler(
        messageSender = FakeTelegramMessageSender(botApi = botApi),
        translationKrate = FakeTranslationKrate(OnboardingTranslation())
    )

    @Test
    fun GIVEN_chat_without_topics_WHEN_info_is_asked_THEN_reply_has_its_id_and_no_topic() = runTest {
        handler.sendChatInfo(messageOf(chat = chatOf(type = "group"), messageId = 55))

        val reply = botApi.sentMessages.single()
        val expected = chatInfo.message(chatId = "$CHAT_ID", topicId = "none", type = "group", isForum = false)
        assertEquals(expected.toMessengerText(), reply.text)
        assertEquals("$CHAT_ID", reply.chatId)
        assertEquals(55, reply.replyToMessageId)
    }

    @Test
    fun GIVEN_forum_topic_WHEN_info_is_asked_THEN_reply_has_the_topic_id() = runTest {
        val message = messageOf(chat = chatOf(isForum = true), threadId = 12, isTopicMessage = true)

        handler.sendChatInfo(message)

        val expected = chatInfo.message(chatId = "$CHAT_ID", topicId = "12", type = "supergroup", isForum = true)
        assertEquals(expected.toMessengerText(), botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_reply_in_a_chat_without_topics_WHEN_info_is_asked_THEN_reply_thread_is_not_a_topic() = runTest {
        handler.sendChatInfo(messageOf(chat = chatOf(isForum = false), threadId = 12, isTopicMessage = false))

        val expected = chatInfo.message(chatId = "$CHAT_ID", topicId = "none", type = "supergroup", isForum = false)
        assertEquals(expected.toMessengerText(), botApi.sentMessages.single().text)
    }
}
