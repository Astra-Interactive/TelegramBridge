@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.telegram.command

import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.api.mapping.asMessage
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.userOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramLinkHandlerTest {
    private val translation = LinkTranslation()
    private val botApi = FakeTelegramBotApi()
    private val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
    private val handler = TelegramLinkHandler(
        linkApi = linkApi,
        messageSender = FakeTelegramMessageSender(botApi),
        translationKrate = FakeTranslationKrate(translation)
    )
    private val threadRoot = Message().apply { messageId = 3 }

    @Test
    fun GIVEN_user_with_username_WHEN_linked_THEN_account_is_linked_and_told_the_result_in_the_thread() = runTest {
        handler.link(1234, messageOf(from = userOf(id = 7L, userName = "steve_mc"), replyTo = threadRoot))

        assertEquals(listOf(1234), linkApi.telegramCodes)
        val telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = "steve_mc", telegramId = 7L)
        assertEquals(listOf(telegramLink), linkApi.telegramLinks)
        val answer = botApi.sentMessages.single()
        assertEquals(LinkResponse.NoCode.asMessage(translation.link).toMessengerText(), answer.text)
        assertEquals("$CHAT_ID", answer.chatId)
        assertEquals(3, answer.replyToMessageId)
    }

    @Test
    fun GIVEN_user_without_username_WHEN_linked_THEN_nothing_is_linked_and_user_is_asked_for_one() = runTest {
        handler.link(1234, messageOf(from = userOf(userName = null)))

        assertTrue(linkApi.telegramCodes.isEmpty())
        val noUsername = LinkResponse.NoUsername.asMessage(translation.link).toMessengerText()
        assertEquals(noUsername, botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_message_without_sender_WHEN_linked_THEN_nothing_happens() = runTest {
        handler.link(1234, messageOf(from = null))

        assertTrue(linkApi.telegramCodes.isEmpty())
        assertTrue(botApi.requests.isEmpty())
    }
}
