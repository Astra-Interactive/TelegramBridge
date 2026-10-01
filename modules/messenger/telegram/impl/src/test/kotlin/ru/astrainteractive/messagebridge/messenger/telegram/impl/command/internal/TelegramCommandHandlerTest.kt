@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal

import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.link.fake.FakeLinkApi
import ru.astrainteractive.messagebridge.link.mapping.asMessage
import ru.astrainteractive.messagebridge.link.model.LinkResponse
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.CHAT_ID
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.userOf
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.model.TelegramCommand
import ru.astrainteractive.messagebridge.messenger.telegram.impl.internal.TelegramMessageSenderImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration

class TelegramCommandHandlerTest {
    private val translation = PluginTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val botApi = FakeTelegramBotApi()
    private val messageSender = TelegramMessageSenderImpl(
        botApi = botApi,
        maxRetries = 0,
        retryDelay = Duration.ZERO,
        logger = RecordingLogger()
    )
    private val linkApi = FakeLinkApi(response = LinkResponse.NoCode)
    private val handler = TelegramCommandHandler(
        messageSender = messageSender,
        onlinePlayersProvider = FakeOnlinePlayersProvider(players = listOf("Steve", "Alex")),
        linkApi = linkApi,
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
    fun GIVEN_update_without_message_WHEN_handled_THEN_nothing_happens() = runTest {
        handler.handle(TelegramCommand.Vanilla, updateOf(message = null))

        assertTrue(botApi.requests.isEmpty())
    }
}
