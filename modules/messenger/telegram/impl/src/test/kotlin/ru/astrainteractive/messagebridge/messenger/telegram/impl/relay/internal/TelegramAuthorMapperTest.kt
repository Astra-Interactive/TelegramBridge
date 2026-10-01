@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.chatOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.userOf
import ru.astrainteractive.messagebridge.messenger.telegram.impl.relay.model.TelegramAuthor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramAuthorMapperTest {
    private val translation = PluginTranslation()
    private val mapper = TelegramAuthorMapper(translationKrate = FakeTranslationKrate(translation))

    @Test
    fun GIVEN_user_with_username_WHEN_mapped_THEN_username_is_the_author() {
        val update = updateOf(messageOf(from = userOf(firstName = "Steve", userName = "steve_mc")))

        assertEquals(TelegramAuthor.Username("steve_mc"), mapper.map(update))
    }

    @Test
    fun GIVEN_user_without_username_WHEN_mapped_THEN_full_name_is_the_author() {
        val update = updateOf(messageOf(from = userOf(firstName = "Steve", lastName = "Jobs")))

        assertEquals(TelegramAuthor.DisplayName("Steve Jobs"), mapper.map(update))
    }

    @Test
    fun GIVEN_user_with_only_first_name_WHEN_mapped_THEN_name_has_no_trailing_space() {
        val update = updateOf(messageOf(from = userOf(firstName = "Steve")))

        assertEquals(TelegramAuthor.DisplayName("Steve"), mapper.map(update))
    }

    @Test
    fun GIVEN_message_on_behalf_of_a_channel_WHEN_mapped_THEN_channel_is_the_author() {
        val channel = chatOf(id = -100500, type = "channel", userName = "server_news")
        val update = updateOf(messageOf(from = userOf(userName = "someone"), senderChat = channel))

        assertEquals(TelegramAuthor.Username("server_news"), mapper.map(update))
    }

    @Test
    fun GIVEN_anonymous_admin_WHEN_mapped_THEN_author_is_anonymous_in_the_default_language() {
        val group = chatOf(userName = null)
        val update = updateOf(messageOf(from = userOf(), senderChat = group))

        val expected = translation.telegram.anonymousAuthor.toMessengerText()
        assertEquals(TelegramAuthor.DisplayName(expected), mapper.map(update))
    }

    @Test
    fun GIVEN_name_with_line_breaks_WHEN_mapped_THEN_they_are_removed() {
        val update = updateOf(messageOf(from = userOf(firstName = "St\neve\t", lastName = "  ")))

        assertEquals(TelegramAuthor.DisplayName("Steve"), mapper.map(update))
    }

    @Test
    fun GIVEN_long_name_WHEN_mapped_THEN_it_is_cut_to_sixteen_characters() {
        val update = updateOf(messageOf(from = userOf(userName = "a_very_long_username_here")))

        assertEquals(TelegramAuthor.Username("a_very_long_user"), mapper.map(update))
    }

    @Test
    fun GIVEN_message_without_sender_WHEN_mapped_THEN_there_is_no_author() {
        assertNull(mapper.map(updateOf(messageOf(from = null))))
        assertNull(mapper.map(updateOf(message = null)))
    }
}
