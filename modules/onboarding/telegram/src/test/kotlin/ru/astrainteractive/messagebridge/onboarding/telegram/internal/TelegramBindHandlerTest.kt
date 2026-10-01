@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner
import org.telegram.telegrambots.meta.api.objects.forum.ForumTopicCreated
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import ru.astrainteractive.messagebridge.core.api.fake.FakeConfigKrate
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.NOW
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.chatOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.userOf
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class TelegramBindHandlerTest {
    private val clock = FakeClock(now = NOW)
    private val bindCodes = BindCodes(clock = clock, lifetime = CODE_LIFETIME, random = Random(1))
    private val botApi = FakeTelegramBotApi()
    private val translation = OnboardingTranslation()
    private val bindTranslation = translation.telegram.bind
    private val unboundConfiguration = configurationOf(chatId = "", topicId = "")
    private val boundTexts: MutableList<LocalizableComponent> = CopyOnWriteArrayList()

    private fun handlerOf(configKrate: FakeConfigKrate) = TelegramBindHandler(
        bindCodes = bindCodes,
        botApi = botApi,
        messageSender = FakeTelegramMessageSender(botApi = botApi),
        configKrate = configKrate,
        translationKrate = FakeTranslationKrate(translation),
        logger = RecordingLogger()
    )

    private fun issueCode(): String = bindCodes.issue { text -> boundTexts += text }.value

    private fun memberIs(member: ChatMember) {
        botApi.answer = { method ->
            if (method is GetChatMember) TelegramRequestResult.Success(member) else FakeTelegramBotApi.delivered(method)
        }
    }

    private fun replies(): List<String> = botApi.sentMessages.map { sendMessage -> sendMessage.text }

    private fun boundTextsShown(): List<String> = boundTexts.map { text -> text.toMessengerText() }

    private fun groupMessage(): Message = messageOf(chat = chatOf(type = "supergroup", title = "Server chat"))

    @Test
    fun GIVEN_admin_sends_the_code_WHEN_bound_THEN_chat_is_saved_and_asker_is_told() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberAdministrator())

        handlerOf(configKrate).bind(issueCode(), groupMessage())

        assertEquals("$CHAT_ID", configKrate.configuration?.tgConfig?.chatID)
        assertEquals("", configKrate.configuration?.tgConfig?.topicID)
        assertEquals(listOf(bindTranslation.success.toMessengerText()), replies())
        val bound = bindTranslation.bound(chat = "Server chat", topic = null)
        assertEquals(listOf(bound.toMessengerText()), boundTextsShown())
    }

    @Test
    fun GIVEN_owner_sends_the_code_WHEN_bound_THEN_chat_is_saved() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberOwner())

        handlerOf(configKrate).bind(issueCode(), groupMessage())

        assertEquals("$CHAT_ID", configKrate.configuration?.tgConfig?.chatID)
    }

    @Test
    fun GIVEN_member_who_is_not_admin_WHEN_code_is_sent_THEN_chat_is_not_bound_and_code_stays_valid() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberMember())
        val code = issueCode()

        handlerOf(configKrate).bind(code, groupMessage())

        assertEquals(listOf(bindTranslation.adminsOnly.toMessengerText()), replies())
        assertTrue(configKrate.saves.isEmpty())
        assertTrue(bindCodes.isValid(code))
        assertTrue(boundTexts.isEmpty())
    }

    @Test
    fun GIVEN_membership_cannot_be_checked_WHEN_code_is_sent_THEN_chat_is_not_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        botApi.answer = { method ->
            if (method is GetChatMember) {
                TelegramRequestResult.Failed(TelegramFailure.NoRights)
            } else {
                FakeTelegramBotApi.delivered(method)
            }
        }

        handlerOf(configKrate).bind(issueCode(), groupMessage())

        assertEquals(listOf(bindTranslation.adminsOnly.toMessengerText()), replies())
        assertTrue(configKrate.saves.isEmpty())
    }

    @Test
    fun GIVEN_bot_disconnects_while_checking_WHEN_code_is_sent_THEN_chat_is_not_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        botApi.answer = { method ->
            if (method is GetChatMember) TelegramRequestResult.NotConnected else FakeTelegramBotApi.delivered(method)
        }

        handlerOf(configKrate).bind(issueCode(), groupMessage())

        assertTrue(configKrate.saves.isEmpty())
    }

    @Test
    fun GIVEN_anonymous_admin_WHEN_code_is_sent_on_behalf_of_the_group_THEN_chat_is_bound_without_a_lookup() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        val group = chatOf(type = "supergroup")
        val message = messageOf(chat = group, senderChat = group, from = userOf(id = 1087968824L))

        handlerOf(configKrate).bind(issueCode(), message)

        assertEquals("$CHAT_ID", configKrate.configuration?.tgConfig?.chatID)
        assertTrue(botApi.requests.none { method -> method is GetChatMember })
    }

    @Test
    fun GIVEN_private_chat_WHEN_code_is_sent_THEN_it_is_bound_without_a_lookup() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        val message = messageOf(chat = chatOf(id = 42L, type = "private", title = null, userName = "steve"))

        handlerOf(configKrate).bind(issueCode(), message)

        assertEquals("42", configKrate.configuration?.tgConfig?.chatID)
        assertEquals(listOf(bindTranslation.bound(chat = "steve", topic = null).toMessengerText()), boundTextsShown())
    }

    @Test
    fun GIVEN_group_message_without_sender_WHEN_code_is_sent_THEN_chat_is_not_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))

        handlerOf(configKrate).bind(issueCode(), messageOf(chat = chatOf(type = "group"), from = null))

        assertEquals(listOf(bindTranslation.adminsOnly.toMessengerText()), replies())
    }

    @Test
    fun GIVEN_unknown_code_WHEN_it_is_sent_THEN_chat_is_not_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        issueCode()

        handlerOf(configKrate).bind("00000000", groupMessage())

        assertEquals(listOf(bindTranslation.invalidCode.toMessengerText()), replies())
        assertTrue(configKrate.saves.isEmpty())
    }

    @Test
    fun GIVEN_expired_code_WHEN_it_is_sent_THEN_chat_is_not_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberAdministrator())
        val code = issueCode()
        clock.now += CODE_LIFETIME + 1.minutes

        handlerOf(configKrate).bind(code, groupMessage())

        assertEquals(listOf(bindTranslation.invalidCode.toMessengerText()), replies())
    }

    @Test
    fun GIVEN_used_code_WHEN_it_is_sent_again_THEN_second_chat_is_not_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberAdministrator())
        val code = issueCode()
        val handler = handlerOf(configKrate)
        handler.bind(code, groupMessage())

        handler.bind(code, messageOf(chat = chatOf(id = -100777, type = "supergroup")))

        assertEquals("$CHAT_ID", configKrate.configuration?.tgConfig?.chatID)
        assertEquals(bindTranslation.invalidCode.toMessengerText(), replies().last())
    }

    @Test
    fun GIVEN_code_sent_in_a_forum_topic_WHEN_bound_THEN_topic_is_saved_under_its_name() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberAdministrator())
        val topicStart = Message().apply { forumTopicCreated = ForumTopicCreated().apply { name = "Minecraft" } }
        val message = messageOf(
            chat = chatOf(type = "supergroup", isForum = true),
            threadId = 12,
            isTopicMessage = true,
            replyTo = topicStart
        )

        handlerOf(configKrate).bind(issueCode(), message)

        assertEquals("12", configKrate.configuration?.tgConfig?.topicID)
        val bound = bindTranslation.bound(chat = "Server chat", topic = "Minecraft")
        assertEquals(listOf(bound.toMessengerText()), boundTextsShown())
    }

    @Test
    fun GIVEN_code_sent_in_a_topic_of_unknown_name_WHEN_bound_THEN_topic_is_named_by_its_id() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        memberIs(ChatMemberAdministrator())
        val forum = chatOf(type = "supergroup", isForum = true)
        val message = messageOf(chat = forum, threadId = 12, isTopicMessage = true)

        handlerOf(configKrate).bind(issueCode(), message)

        val bound = bindTranslation.bound(chat = "Server chat", topic = "12")
        assertEquals(listOf(bound.toMessengerText()), boundTextsShown())
    }

    @Test
    fun GIVEN_config_file_with_an_error_WHEN_bound_THEN_nothing_is_saved_and_both_sides_are_told() = runTest {
        val configKrate = FakeConfigKrate(Result.failure(IllegalStateException("line 3, column 1: bad indent")))
        memberIs(ChatMemberAdministrator())

        handlerOf(configKrate).bind(issueCode(), groupMessage())

        assertTrue(configKrate.saves.isEmpty())
        assertEquals(listOf(bindTranslation.saveFailed.toMessengerText()), replies())
        val notBound = bindTranslation.notBound(chat = "Server chat", error = "line 3, column 1: bad indent")
        assertEquals(listOf(notBound.toMessengerText()), boundTextsShown())
    }

    @Test
    fun GIVEN_two_admins_send_one_code_at_once_WHEN_bound_THEN_only_one_chat_is_bound() = runTest {
        val configKrate = FakeConfigKrate(Result.success(unboundConfiguration))
        botApi.answer = { method ->
            yield()
            if (method is GetChatMember) {
                TelegramRequestResult.Success(ChatMemberAdministrator())
            } else {
                FakeTelegramBotApi.delivered(method)
            }
        }
        val handler = handlerOf(configKrate)
        val code = issueCode()

        launch { handler.bind(code, messageOf(chat = chatOf(id = -100111, type = "supergroup"))) }
        launch { handler.bind(code, messageOf(chat = chatOf(id = -100222, type = "supergroup"))) }
        testScheduler.advanceUntilIdle()

        assertEquals(1, configKrate.saves.size)
        assertEquals(1, boundTexts.size)
        assertEquals(1, replies().count { reply -> reply == bindTranslation.invalidCode.toMessengerText() })
    }

    private companion object {
        val CODE_LIFETIME = 10.minutes
    }
}
