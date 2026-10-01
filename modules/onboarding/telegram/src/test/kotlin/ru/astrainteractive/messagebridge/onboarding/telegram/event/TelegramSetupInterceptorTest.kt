@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.event

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import ru.astrainteractive.messagebridge.core.api.fake.FakeConfigKrate
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.BOT_USER_NAME
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.NOW
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramRequestResult
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramBindHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramSetupCommandParser
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class TelegramSetupInterceptorTest {
    private val translationKrate = FakeTranslationKrate(OnboardingTranslation())
    private val botApi = FakeTelegramBotApi()
    private val messageSender = FakeTelegramMessageSender(botApi = botApi)
    private val configKrate = FakeConfigKrate(Result.success(configurationOf(chatId = "", topicId = "")))
    private val bindCodes = BindCodes(clock = FakeClock(now = NOW), lifetime = 10.minutes, random = Random(1))

    private fun TestScope.intercept(update: Update): Boolean {
        val interceptor = TelegramSetupInterceptor(
            scope = this,
            commandParser = TelegramSetupCommandParser(botUserName = { BOT_USER_NAME }),
            bindHandler = TelegramBindHandler(
                bindCodes = bindCodes,
                botApi = botApi,
                messageSender = messageSender,
                configKrate = configKrate,
                translationKrate = translationKrate,
                logger = RecordingLogger()
            ),
            chatInfoHandler = TelegramChatInfoHandler(
                messageSender = messageSender,
                translationKrate = translationKrate,
                logger = RecordingLogger()
            )
        )
        val isTaken = interceptor.intercept(update)
        advanceUntilIdle()
        return isTaken
    }

    @Test
    fun GIVEN_minfo_WHEN_intercepted_THEN_it_is_taken_and_chat_ids_are_sent() = runTest {
        val isTaken = intercept(updateOf(messageOf(text = "/minfo")))

        assertTrue(isTaken)
        assertTrue("$CHAT_ID" in botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_bind_with_issued_code_WHEN_intercepted_THEN_chat_is_bound() = runTest {
        botApi.answer = { method ->
            if (method is GetChatMember) {
                TelegramRequestResult.Success(ChatMemberAdministrator())
            } else {
                FakeTelegramBotApi.delivered(method)
            }
        }
        val code = bindCodes.issue(onBound = { _ -> }).value

        val isTaken = intercept(updateOf(messageOf(text = "/bind $code")))

        assertTrue(isTaken)
        assertEquals("$CHAT_ID", configKrate.configuration?.tgConfig?.chatID)
    }

    @Test
    fun GIVEN_chat_message_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        val isTaken = intercept(updateOf(messageOf(text = "hello")))

        assertFalse(isTaken)
        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_message_without_text_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(updateOf(messageOf(text = null))))
    }

    @Test
    fun GIVEN_update_without_message_WHEN_intercepted_THEN_it_is_left_to_the_relay() = runTest {
        assertFalse(intercept(updateOf(message = null)))
    }
}
