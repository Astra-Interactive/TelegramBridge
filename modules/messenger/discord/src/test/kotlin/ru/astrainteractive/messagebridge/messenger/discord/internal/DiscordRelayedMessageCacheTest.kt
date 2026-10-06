@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscordRelayedMessageCacheTest {
    private val text = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello",
        ref = MessageRef.Minecraft(messageId = "mc-1")
    )
    private val otherText = text.copy(text = "bye", ref = MessageRef.Minecraft(messageId = "mc-2"))

    private fun cache(capacity: Int): DiscordRelayedMessageCache {
        val config = PluginConfiguration(jdaConfig = PluginConfiguration.JdaConfig(relayedMessageCacheSize = capacity))
        return DiscordRelayedMessageCache(
            configKrate = DefaultMutableKrate(factory = { config }, loader = { null }).asCachedKrate()
        )
    }

    @Test
    fun GIVEN_relayed_message_WHEN_found_by_its_id_THEN_returns_its_text() = runTest {
        val cache = cache(capacity = 10)

        cache.remember(messageId = 1L, text = text)

        assertEquals(text, cache.find(messageId = 1L))
    }

    @Test
    fun GIVEN_message_never_relayed_WHEN_found_THEN_returns_null() = runTest {
        val cache = cache(capacity = 10)

        cache.remember(messageId = 1L, text = text)

        assertNull(cache.find(messageId = 2L))
    }

    @Test
    fun GIVEN_more_messages_than_capacity_WHEN_found_THEN_only_the_latest_are_kept() = runTest {
        val cache = cache(capacity = 2)

        cache.remember(messageId = 1L, text = text)
        cache.remember(messageId = 2L, text = text)
        cache.remember(messageId = 3L, text = text)

        assertNull(cache.find(messageId = 1L))
        assertEquals(text, cache.find(messageId = 2L))
        assertEquals(text, cache.find(messageId = 3L))
    }

    @Test
    fun GIVEN_relayed_message_WHEN_copy_of_its_origin_is_asked_THEN_returns_its_message_id() = runTest {
        val cache = cache(capacity = 10)

        cache.remember(messageId = 1L, text = text)

        assertEquals(1L, cache.copyOf(origin = text.ref))
    }

    @Test
    fun GIVEN_origin_never_relayed_WHEN_its_copy_is_asked_THEN_returns_null() = runTest {
        val cache = cache(capacity = 10)

        cache.remember(messageId = 1L, text = text)

        assertNull(cache.copyOf(origin = otherText.ref))
    }

    @Test
    fun GIVEN_relayed_message_pushed_out_by_capacity_WHEN_copy_of_its_origin_is_asked_THEN_returns_null() = runTest {
        val cache = cache(capacity = 1)

        cache.remember(messageId = 1L, text = text)
        cache.remember(messageId = 2L, text = otherText)

        assertNull(cache.copyOf(origin = text.ref))
        assertEquals(2L, cache.copyOf(origin = otherText.ref))
    }

    @Test
    fun GIVEN_origin_relayed_twice_WHEN_the_first_copy_is_pushed_out_THEN_the_second_copy_is_still_known() = runTest {
        val cache = cache(capacity = 2)

        cache.remember(messageId = 1L, text = text)
        cache.remember(messageId = 2L, text = text)
        cache.remember(messageId = 3L, text = otherText)

        assertEquals(2L, cache.copyOf(origin = text.ref))
    }

    @Test
    fun GIVEN_messages_relayed_from_many_threads_WHEN_all_are_done_THEN_exactly_capacity_messages_are_kept() = runTest {
        val cache = cache(capacity = CAPACITY)

        withContext(Dispatchers.Default) {
            repeat(THREADS) { threadIndex ->
                launch {
                    repeat(MESSAGES_PER_THREAD) { index ->
                        val messageId = (threadIndex * MESSAGES_PER_THREAD + index).toLong()
                        cache.remember(messageId = messageId, text = text)
                        cache.find(messageId = index.toLong())
                    }
                }
            }
        }

        val keptCount = (0 until THREADS * MESSAGES_PER_THREAD).count { messageId ->
            cache.find(messageId = messageId.toLong()) != null
        }
        assertEquals(CAPACITY, keptCount)
    }

    private companion object {
        const val CAPACITY = 100
        const val THREADS = 8
        const val MESSAGES_PER_THREAD = 2000
    }
}
