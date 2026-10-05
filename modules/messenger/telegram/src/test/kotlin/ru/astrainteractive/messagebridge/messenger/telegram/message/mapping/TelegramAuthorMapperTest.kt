@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.message.mapping

import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.telegram.message.model.TelegramAuthor
import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramAuthorMapperTest {
    private val namelessChatPost = Message().apply {
        messageId = 1
        senderChat = Chat(CHANNEL_ID, "channel")
    }

    private fun mapper(translation: PluginTranslation): TelegramAuthorMapper {
        return TelegramAuthorMapper(
            translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
        )
    }

    @Test
    fun GIVEN_translated_anonymous_name_WHEN_chat_without_any_name_posts_THEN_author_has_that_name() {
        val translation = PluginTranslation(
            chat = PluginTranslation.Chat(anonymousAuthor = LocalizedText.shared("Ghost"))
        )

        val author = mapper(translation).map(namelessChatPost)

        assertEquals(TelegramAuthor.DisplayName("Ghost"), author)
    }

    @Test
    fun GIVEN_default_translation_WHEN_chat_without_any_name_posts_THEN_author_is_anonymous() {
        val author = mapper(PluginTranslation()).map(namelessChatPost)

        assertEquals(TelegramAuthor.DisplayName("Anonymous"), author)
    }

    private companion object {
        const val CHANNEL_ID = -100123L
    }
}
