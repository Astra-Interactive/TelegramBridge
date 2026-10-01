@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.relay.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramRelayedMessageCacheTest {
    private val text = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello"
    )

    @Test
    fun GIVEN_relayed_message_WHEN_found_by_its_chat_and_id_THEN_returns_its_text() = runTest {
        val cache = TelegramRelayedMessageCache(capacity = 10)

        cache.remember(chatId = CHAT_ID, messageId = 1, text = text)

        assertEquals(text, cache.find(chatId = CHAT_ID, messageId = 1))
    }

    @Test
    fun GIVEN_relayed_message_WHEN_same_id_is_found_in_another_chat_THEN_returns_null() = runTest {
        val cache = TelegramRelayedMessageCache(capacity = 10)

        cache.remember(chatId = CHAT_ID, messageId = 1, text = text)

        assertNull(cache.find(chatId = OTHER_CHAT_ID, messageId = 1))
    }

    @Test
    fun GIVEN_more_messages_than_capacity_WHEN_found_THEN_only_the_latest_are_kept() = runTest {
        val cache = TelegramRelayedMessageCache(capacity = 2)

        cache.remember(chatId = CHAT_ID, messageId = 1, text = text)
        cache.remember(chatId = CHAT_ID, messageId = 2, text = text)
        cache.remember(chatId = CHAT_ID, messageId = 3, text = text)

        assertNull(cache.find(chatId = CHAT_ID, messageId = 1))
        assertEquals(text, cache.find(chatId = CHAT_ID, messageId = 2))
        assertEquals(text, cache.find(chatId = CHAT_ID, messageId = 3))
    }

    @Test
    fun GIVEN_messages_relayed_from_many_threads_WHEN_all_are_done_THEN_exactly_capacity_messages_are_kept() = runTest {
        val cache = TelegramRelayedMessageCache(capacity = CAPACITY)

        withContext(Dispatchers.Default) {
            repeat(THREADS) { threadIndex ->
                launch {
                    repeat(MESSAGES_PER_THREAD) { index ->
                        val messageId = threadIndex * MESSAGES_PER_THREAD + index
                        cache.remember(chatId = CHAT_ID, messageId = messageId, text = text)
                        cache.find(chatId = CHAT_ID, messageId = index)
                    }
                }
            }
        }

        val keptCount = (0 until THREADS * MESSAGES_PER_THREAD).count { messageId ->
            cache.find(chatId = CHAT_ID, messageId = messageId) != null
        }
        assertEquals(CAPACITY, keptCount)
    }

    private companion object {
        const val CHAT_ID = -1001L
        const val OTHER_CHAT_ID = -1002L
        const val CAPACITY = 100
        const val THREADS = 8
        const val MESSAGES_PER_THREAD = 2000
    }
}
