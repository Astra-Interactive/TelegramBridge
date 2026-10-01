@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.command.internal

import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.link.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.messenger.telegram.command.model.TelegramCommand
import ru.astrainteractive.messagebridge.messenger.telegram.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeClock
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeConfigKrate
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.messenger.telegram.fake.NOW
import ru.astrainteractive.messagebridge.messenger.telegram.fake.RecordingLogger
import ru.astrainteractive.messagebridge.messenger.telegram.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.userOf
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramBindHandler
import ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal.TelegramChatInfoHandler
import ru.astrainteractive.messagebridge.messenger.telegram.request.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.request.internal.TelegramMessageSender
import ru.astrainteractive.messagebridge.messenger.telegram.request.model.TelegramRequestResult
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TelegramCommandHandlerTest {
    private val translation = PluginTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val botApi = FakeTelegramBotApi()
    private val messageSender = TelegramMessageSender(
        botApi = botApi,
        maxRetries = 0,
        retryDelay = Duration.ZERO,
        logger = RecordingLogger()
    )
    private val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
    private val configKrate = FakeConfigKrate(Result.success(configurationOf(chatId = "")))
    private val bindCodes = BindCodes(
        clock = FakeClock(now = NOW),
        lifetime = 10.minutes,
        random = Random(1)
    )
    private val handler = TelegramCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = FakeOnlinePlayersProvider(players = listOf("Steve", "Alex")),
        linkApi = linkApi,
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
        ),
        translationKrate = translationKrate
    )
    private val threadRoot = Message().apply { messageId = 3 }

    @Test
    fun GIVEN_vanilla_WHEN_handled_THEN_online_players_are_listed_in_the_same_thread() = runTest {
        handler.handle(TelegramCommand.Vanilla, updateOf(messageOf(replyTo = threadRoot)))

        val answer = botApi.sentMessages.single()
        val players = translation.onlinePlayers.message(count = 2, players = "Steve, Alex")
        assertEquals(players.toMessengerText(), answer.text)
        assertEquals("$CHAT_ID", answer.chatId)
        assertEquals(3, answer.replyToMessageId)
    }

    @Test
    fun GIVEN_link_code_WHEN_handled_THEN_account_is_linked_and_user_is_told_the_result() = runTest {
        val user = userOf(userName = "steve_mc")

        handler.handle(TelegramCommand.Link(1234), updateOf(messageOf(from = user)))

        assertEquals(listOf(1234), linkApi.telegramCodes)
        assertEquals(listOf(user), linkApi.telegramUsers)
        val noCode = LinkResponse.NoCode.asMessage(translation.link)
        assertEquals(noCode.toMessengerText(), botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_link_without_sender_WHEN_handled_THEN_nothing_is_linked() = runTest {
        handler.handle(TelegramCommand.Link(1234), updateOf(messageOf(from = null)))

        assertTrue(linkApi.telegramCodes.isEmpty())
        assertTrue(botApi.requests.isEmpty())
    }

    @Test
    fun GIVEN_chat_info_WHEN_handled_THEN_chat_ids_are_sent() = runTest {
        handler.handle(TelegramCommand.ChatInfo, updateOf(messageOf()))

        assertTrue("$CHAT_ID" in botApi.sentMessages.single().text)
    }

    @Test
    fun GIVEN_bind_code_WHEN_handled_THEN_chat_is_bound() = runTest {
        botApi.answer = { method ->
            if (method is GetChatMember) {
                TelegramRequestResult.Success(ChatMemberAdministrator())
            } else {
                FakeTelegramBotApi.delivered(method)
            }
        }
        val code = bindCodes.issue(onBound = { _ -> }).value

        handler.handle(TelegramCommand.Bind(code), updateOf(messageOf()))

        assertEquals("$CHAT_ID", configKrate.configuration?.tgConfig?.chatID)
    }

    @Test
    fun GIVEN_update_without_message_WHEN_handled_THEN_nothing_happens() = runTest {
        handler.handle(TelegramCommand.Vanilla, updateOf(message = null))

        assertTrue(botApi.requests.isEmpty())
    }
}
