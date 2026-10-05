@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordWebhookMessageMapperTest {
    private val steveMessage = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello"
    )
    private val telegramMessage = Text.Telegram(author = "steve_tg", text = "hi", authorId = 1L, reply = null)
    private val linkedMember: Member = jdaFake(
        mapOf(
            "getEffectiveName" to "Stevie",
            "getEffectiveAvatarUrl" to "https://cdn.discordapp.com/avatars/stevie.png"
        )
    )

    private fun mapper(translation: PluginTranslation): DiscordWebhookMessageMapper {
        val translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
        return DiscordWebhookMessageMapper(translationKrate)
    }

    @Test
    fun GIVEN_default_translation_WHEN_minecraft_message_is_mapped_THEN_username_is_the_source_tag_and_the_author() {
        val message = mapper(PluginTranslation()).map(steveMessage, member = null)

        assertEquals("[MC] Steve", message.username)
    }

    @Test
    fun GIVEN_author_linked_to_a_server_member_WHEN_message_is_mapped_THEN_username_is_the_member_name() {
        val message = mapper(PluginTranslation()).map(telegramMessage, linkedMember)

        assertEquals("[TG] Stevie", message.username)
    }

    @Test
    fun GIVEN_translated_username_format_WHEN_message_is_mapped_THEN_username_follows_that_format() {
        val translation = PluginTranslation(
            chat = PluginTranslation.Chat(toDiscordUsername = LocalizedText.shared("%dao% via %from%"))
        )

        val message = mapper(translation).map(steveMessage, member = null)

        assertEquals("Steve via MC", message.username)
    }

    @Test
    fun GIVEN_author_name_with_markup_WHEN_message_is_mapped_THEN_username_shows_the_markup_as_text() {
        val markupAuthor = telegramMessage.copy(author = "<red>Tom & &aJerry")

        val message = mapper(PluginTranslation()).map(markupAuthor, member = null)

        assertEquals("[TG] <red>Tom & &aJerry", message.username)
    }
}
