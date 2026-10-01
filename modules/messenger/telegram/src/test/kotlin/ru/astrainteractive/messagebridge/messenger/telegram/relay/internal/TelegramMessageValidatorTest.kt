@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.relay.internal

import kotlinx.coroutines.flow.MutableStateFlow
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.messenger.telegram.fake.chatOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.messageOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.updateOf
import ru.astrainteractive.messagebridge.messenger.telegram.fake.userOf
import ru.astrainteractive.messagebridge.messenger.telegram.relay.model.TelegramMessageValidation
import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramMessageValidatorTest {
    private val configuration = configurationOf(maxMessageLength = MAX_LENGTH, displayNameRegex = "[A-Za-z ]+")
    private val validator = TelegramMessageValidator(
        configFlow = MutableStateFlow(configuration),
        authorMapper = TelegramAuthorMapper(translationKrate = FakeTranslationKrate(PluginTranslation()))
    )

    @Test
    fun GIVEN_message_from_nobody_WHEN_validated_THEN_it_has_no_author() {
        assertEquals(TelegramMessageValidation.NoAuthor, validator.map(updateOf(messageOf(from = null))))
    }

    @Test
    fun GIVEN_display_name_rejected_by_regex_WHEN_validated_THEN_name_is_illegal() {
        val update = updateOf(messageOf(from = userOf(firstName = "Стив")))

        assertEquals(TelegramMessageValidation.IllegalDisplayName, validator.map(update))
    }

    @Test
    fun GIVEN_username_the_regex_would_reject_WHEN_validated_THEN_it_is_allowed() {
        val update = updateOf(messageOf(from = userOf(firstName = "Стив", userName = "steve_42")))

        val validation = validator.map(update)

        assertEquals(TelegramMessageValidation.Valid(author = "steve_42", text = "hello", authorId = 7L), validation)
    }

    @Test
    fun GIVEN_message_without_text_WHEN_validated_THEN_there_is_nothing_to_relay() {
        assertEquals(TelegramMessageValidation.NoText, validator.map(updateOf(messageOf(text = null))))
        assertEquals(TelegramMessageValidation.NoText, validator.map(updateOf(messageOf(text = "  "))))
    }

    @Test
    fun GIVEN_text_longer_than_the_limit_WHEN_validated_THEN_it_is_too_long() {
        val update = updateOf(messageOf(text = "a".repeat(MAX_LENGTH + 1)))

        assertEquals(TelegramMessageValidation.TooLong, validator.map(update))
    }

    @Test
    fun GIVEN_text_of_exactly_the_limit_WHEN_validated_THEN_it_is_valid() {
        val text = "a".repeat(MAX_LENGTH)

        val validation = validator.map(updateOf(messageOf(text = text, from = userOf(id = 9L, firstName = "Alex"))))

        assertEquals(TelegramMessageValidation.Valid(author = "Alex", text = text, authorId = 9L), validation)
    }

    @Test
    fun GIVEN_channel_post_without_user_WHEN_validated_THEN_author_id_is_unknown() {
        val channel = chatOf(type = "channel", userName = "server_news")

        val validation = validator.map(updateOf(messageOf(from = null, senderChat = channel)))

        assertEquals(TelegramMessageValidation.Valid(author = "server_news", text = "hello", authorId = 0L), validation)
    }

    private companion object {
        const val MAX_LENGTH = 10
    }
}
