@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.request.internal

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.astrainteractive.messagebridge.core.api.fake.LogLine
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramRequestResult
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class TelegramMessageSenderImplTest {
    private val botApi = FakeTelegramBotApi()
    private val logger = RecordingLogger()
    private val sender = TelegramMessageSenderImpl(
        botApi = botApi,
        maxRetries = MAX_RETRIES,
        retryDelay = RETRY_DELAY,
        logger = logger
    )
    private val serverError = TelegramRequestResult.Failed(TelegramFailure.ServerError(502))

    private fun failFirst(times: Int, failure: TelegramRequestResult<*>) {
        val attempts = AtomicInteger()
        botApi.answer = { method ->
            if (attempts.incrementAndGet() <= times) failure else FakeTelegramBotApi.delivered(method)
        }
    }

    @Test
    fun GIVEN_reply_WHEN_sent_THEN_it_answers_the_given_message() = runTest {
        sender.send(chatId = "-100", text = "hello", replyToMessageId = 17)

        val sent = botApi.sentMessages.single()
        assertEquals("-100", sent.chatId)
        assertEquals("hello", sent.text)
        assertEquals(17, sent.replyToMessageId)
    }

    @Test
    fun GIVEN_transient_failure_WHEN_sending_THEN_message_is_sent_after_a_delay() = runTest {
        failFirst(times = 2, failure = serverError)

        sender.send(chatId = "-100", text = "hello", replyToMessageId = null)

        assertEquals(3, botApi.sentMessages.size)
        assertEquals(RETRY_DELAY.inWholeMilliseconds * 2, currentTime)
        assertTrue(logger.messagesOf(LogLine.Level.ERROR).isEmpty())
    }

    @Test
    fun GIVEN_failure_that_stays_WHEN_sending_THEN_it_gives_up_and_logs_it() = runTest {
        failFirst(times = Int.MAX_VALUE, failure = serverError)

        sender.send(chatId = "-100", text = "hello", replyToMessageId = null)

        assertEquals(MAX_RETRIES + 1, botApi.sentMessages.size)
        assertEquals(1, logger.messagesOf(LogLine.Level.ERROR).size)
    }

    @Test
    fun GIVEN_failure_that_does_not_pass_WHEN_deleting_THEN_it_is_not_repeated() = runTest {
        failFirst(times = Int.MAX_VALUE, failure = TelegramRequestResult.Failed(TelegramFailure.NoRights))

        sender.delete(chatId = "-100", messageId = 5)

        assertEquals(1, botApi.deletedMessages.size)
        assertTrue("NoRights" in logger.messagesOf(LogLine.Level.ERROR).single())
    }

    @Test
    fun GIVEN_no_bot_WHEN_sending_THEN_nothing_is_repeated_nor_reported_as_an_error() = runTest {
        failFirst(times = Int.MAX_VALUE, failure = TelegramRequestResult.NotConnected)

        sender.send(chatId = "-100", text = "hello", replyToMessageId = null)

        assertEquals(1, botApi.requests.filterIsInstance<SendMessage>().size)
        assertTrue(logger.messagesOf(LogLine.Level.ERROR).isEmpty())
    }

    private companion object {
        const val MAX_RETRIES = 3
        val RETRY_DELAY = 500.milliseconds
    }
}
