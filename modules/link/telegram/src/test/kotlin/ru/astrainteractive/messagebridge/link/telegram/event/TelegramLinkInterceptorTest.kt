@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.telegram.event

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.Update
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.link.api.config.LinkTranslation
import ru.astrainteractive.messagebridge.link.api.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.api.model.LinkResponse
import ru.astrainteractive.messagebridge.link.telegram.command.TelegramLinkHandler
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.BOT_USER_NAME
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.userOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelegramLinkInterceptorTest {
    private val botApi = FakeTelegramBotApi()
    private val linkApi = FakeLinkApi(response = LinkResponse.NoCode)

    private fun linkUpdate(text: String?): Update {
        return updateOf(messageOf(text = text, from = userOf(userName = "steve_mc")))
    }

    private fun TestScope.intercept(update: Update, bridgedChatId: String = "$CHAT_ID"): Boolean {
        val interceptor = TelegramLinkInterceptor(
            scope = this,
            configFlow = MutableStateFlow(configurationOf(chatId = bridgedChatId)),
            botUserName = { BOT_USER_NAME },
            linkHandler = TelegramLinkHandler(
                linkApi = linkApi,
                messageSender = FakeTelegramMessageSender(botApi),
                translationKrate = FakeTranslationKrate(LinkTranslation())
            )
        )
        val isTaken = interceptor.intercept(update)
        advanceUntilIdle()
        return isTaken
    }

    @Test
    fun GIVEN_link_code_in_the_bridged_chat_WHEN_intercepted_THEN_the_code_is_linked() = runTest {
        assertTrue(intercept(linkUpdate("/link 1234")))

        assertEquals(listOf(1234), linkApi.telegramCodes)
        assertEquals(1, botApi.sentMessages.size)
    }

    @Test
    fun GIVEN_link_addressed_to_this_bot_WHEN_intercepted_THEN_the_code_is_linked() = runTest {
        assertTrue(intercept(linkUpdate("/link@$BOT_USER_NAME 1234")))

        assertEquals(listOf(1234), linkApi.telegramCodes)
    }

    @Test
    fun GIVEN_link_without_a_number_WHEN_intercepted_THEN_an_invalid_code_is_tried() = runTest {
        assertTrue(intercept(linkUpdate("/link abc")))

        assertEquals(listOf(-1), linkApi.telegramCodes)
    }

    @Test
    fun GIVEN_link_addressed_to_another_bot_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(linkUpdate("/link@OtherBot 1234")))

        assertTrue(linkApi.telegramCodes.isEmpty())
    }

    @Test
    fun GIVEN_link_in_another_chat_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(linkUpdate("/link 1234"), bridgedChatId = "42"))

        assertTrue(linkApi.telegramCodes.isEmpty())
    }

    @Test
    fun GIVEN_chat_message_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(linkUpdate("hello")))
    }

    @Test
    fun GIVEN_message_without_text_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(linkUpdate(text = null)))
    }

    @Test
    fun GIVEN_update_without_message_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(updateOf(message = null)))
    }
}
