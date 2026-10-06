@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.entities.Member
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordWebhookMessageMapperTest {
    private val steveMessage = Text.Minecraft(
        author = "Steve",
        uuid = "8667ba71-b85a-4004-af54-457a9734eed7",
        text = "hello",
        ref = MessageRef.Minecraft(messageId = "mc-1")
    )
    private val telegramMessage = Text.Telegram(
        author = "steve_tg",
        text = "hi",
        authorId = 1L,
        reply = null,
        ref = MessageRef.Telegram(chatId = -1001L, messageId = 1)
    )
    private val telegramReply = telegramMessage.copy(
        reply = Text.Reply(author = "Alex", authorId = null, text = "see you at spawn", target = null)
    )
    private val linkedMember: Member = jdaFake(
        mapOf(
            "getEffectiveName" to "Stevie",
            "getEffectiveAvatarUrl" to "https://cdn.discordapp.com/avatars/stevie.png"
        )
    )

    private fun mapper(
        translation: PluginTranslation = PluginTranslation(),
        config: PluginConfiguration = PluginConfiguration()
    ): DiscordWebhookMessageMapper {
        return DiscordWebhookMessageMapper(
            configKrate = DefaultMutableKrate(factory = { config }, loader = { null }).asCachedKrate(),
            translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
        )
    }

    @Test
    fun GIVEN_default_translation_WHEN_minecraft_message_is_mapped_THEN_username_is_the_source_tag_and_the_author() {
        val message = mapper().map(steveMessage, member = null, replyJumpUrl = null)

        assertEquals("[MC] Steve", message.username)
    }

    @Test
    fun GIVEN_author_linked_to_a_server_member_WHEN_message_is_mapped_THEN_username_is_the_member_name() {
        val message = mapper().map(telegramMessage, linkedMember, replyJumpUrl = null)

        assertEquals("[TG] Stevie", message.username)
    }

    @Test
    fun GIVEN_translated_username_format_WHEN_message_is_mapped_THEN_username_follows_that_format() {
        val translation = PluginTranslation(
            chat = PluginTranslation.Chat(toDiscordUsername = LocalizedText.shared("%dao% via %from%"))
        )

        val message = mapper(translation).map(steveMessage, member = null, replyJumpUrl = null)

        assertEquals("Steve via MC", message.username)
    }

    @Test
    fun GIVEN_author_name_with_markup_WHEN_message_is_mapped_THEN_username_shows_the_markup_as_text() {
        val markupAuthor = telegramMessage.copy(author = "<red>Tom & &aJerry")

        val message = mapper().map(markupAuthor, member = null, replyJumpUrl = null)

        assertEquals("[TG] <red>Tom & &aJerry", message.username)
    }

    @Test
    fun GIVEN_message_without_reply_WHEN_mapped_THEN_content_is_its_text() {
        val message = mapper().map(steveMessage, member = null, replyJumpUrl = null)

        assertEquals("hello", message.content)
    }

    @Test
    fun GIVEN_reply_with_jump_url_WHEN_mapped_THEN_subtext_line_links_the_replied_player_above_the_text() {
        val message = mapper().map(telegramReply, member = null, replyJumpUrl = JUMP_URL)

        assertEquals("-# ↪ [Alex]($JUMP_URL): see you at spawn\nhi", message.content)
    }

    @Test
    fun GIVEN_reply_without_jump_url_WHEN_mapped_THEN_subtext_line_names_the_replied_player() {
        val message = mapper().map(telegramReply, member = null, replyJumpUrl = null)

        assertEquals("-# ↪ Alex: see you at spawn\nhi", message.content)
    }

    @Test
    fun GIVEN_replied_text_with_markdown_and_line_breaks_WHEN_mapped_THEN_quote_is_one_escaped_line() {
        val reply = telegramReply.copy(reply = telegramReply.reply?.copy(text = "*bold*\n\n_it_ [x] `c`"))

        val message = mapper().map(reply, member = null, replyJumpUrl = null)

        assertEquals("""-# ↪ Alex: \*bold\* \_it\_ \[x\] \`c\`""" + "\nhi", message.content)
    }

    @Test
    fun GIVEN_replied_text_longer_than_the_preview_length_WHEN_mapped_THEN_quote_is_cut() {
        val config = PluginConfiguration(jdaConfig = PluginConfiguration.JdaConfig(replyPreviewLength = 5))

        val message = mapper(config = config).map(telegramReply, member = null, replyJumpUrl = null)

        assertEquals("-# ↪ Alex: see y…\nhi", message.content)
    }

    @Test
    fun GIVEN_reply_to_media_without_caption_WHEN_mapped_THEN_quote_names_the_media() {
        val reply = telegramReply.copy(reply = telegramReply.reply?.copy(text = ""))

        val message = mapper().map(reply, member = null, replyJumpUrl = null)

        assertEquals("-# ↪ Alex: [media]\nhi", message.content)
    }

    @Test
    fun GIVEN_replied_player_named_like_a_mention_WHEN_mapped_THEN_quote_cannot_ping() {
        val reply = telegramReply.copy(reply = telegramReply.reply?.copy(author = "@every_one"))

        val message = mapper().map(reply, member = null, replyJumpUrl = null)

        assertEquals("-# ↪ every\\_one: see you at spawn\nhi", message.content)
    }

    @Test
    fun GIVEN_translated_reply_format_WHEN_mapped_THEN_content_follows_that_format() {
        val translation = PluginTranslation(
            chat = PluginTranslation.Chat(toDiscordReply = LocalizedText.shared("> %quote%\n%message%"))
        )

        val message = mapper(translation).map(telegramReply, member = null, replyJumpUrl = null)

        assertEquals("> Alex: see you at spawn\nhi", message.content)
    }

    private companion object {
        const val JUMP_URL = "https://discord.com/channels/1/2/3"
    }
}
