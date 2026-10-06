@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.message.mapping

import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.entities.SelfUser
import net.dv8tion.jda.api.entities.User
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.api.model.MessageRef
import ru.astrainteractive.messagebridge.messenger.api.model.Text
import ru.astrainteractive.messagebridge.messenger.discord.fake.jdaFake
import ru.astrainteractive.messagebridge.messenger.discord.internal.DiscordRelayedMessageCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscordReplyMapperTest {
    private val configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null })
        .asCachedKrate()
    private val translationKrate = DefaultMutableKrate(factory = { PluginTranslation() }, loader = { null })
        .asCachedKrate()
    private val relayedMessageCache = DiscordRelayedMessageCache(configKrate = configKrate)
    private val mapper = DiscordReplyMapper(
        translationKrate = translationKrate,
        relayedMessageCache = relayedMessageCache
    )
    private val selfUser: SelfUser = jdaFake(mapOf("getIdLong" to BOT_ID))
    private val jda: JDA = jdaFake(mapOf("getSelfUser" to selfUser))
    private val relayedTelegramMessage = Text.Telegram(
        author = "steve_tg",
        text = "hello from telegram",
        authorId = STEVE_ID,
        reply = null,
        ref = MessageRef.Telegram(chatId = -1001L, messageId = 10)
    )

    private fun user(name: String, id: Long): User = jdaFake(mapOf("getName" to name, "getIdLong" to id))

    private fun member(nickname: String?): Member = jdaFake(mapOf("getNickname" to nickname))

    private fun repliedMessage(
        author: User,
        member: Member?,
        isWebhook: Boolean,
        content: String = "hello",
        embeds: List<MessageEmbed> = emptyList()
    ): Message = jdaFake(
        mapOf(
            "getAuthor" to author,
            "getMember" to member,
            "isWebhookMessage" to isWebhook,
            "getContentRaw" to content,
            "getIdLong" to REPLIED_ID,
            "getJDA" to jda,
            "getEmbeds" to embeds
        )
    )

    private fun message(type: MessageType, referenced: Message?): Message = jdaFake(
        mapOf(
            "getType" to type,
            "getReferencedMessage" to referenced
        )
    )

    private fun replyTo(replied: Message): Message = message(type = MessageType.INLINE_REPLY, referenced = replied)

    @Test
    fun GIVEN_message_that_is_not_a_reply_WHEN_mapped_THEN_returns_null() = runTest {
        assertNull(mapper.map(message(type = MessageType.DEFAULT, referenced = null)))
    }

    @Test
    fun GIVEN_reply_whose_original_discord_did_not_send_WHEN_mapped_THEN_returns_null() = runTest {
        assertNull(mapper.map(message(type = MessageType.INLINE_REPLY, referenced = null)))
    }

    @Test
    fun GIVEN_reply_to_member_with_nickname_WHEN_mapped_THEN_reply_names_nickname_and_points_at_the_message() =
        runTest {
            val replied = repliedMessage(author = user("steve", STEVE_ID), member = member("Stevie"), isWebhook = false)

            val reply = mapper.map(replyTo(replied))

            assertEquals(
                Text.Reply(author = "Stevie", authorId = STEVE_ID, text = "hello", target = REPLIED_REF),
                reply
            )
        }

    @Test
    fun GIVEN_reply_to_member_without_nickname_WHEN_mapped_THEN_reply_names_user() = runTest {
        val replied = repliedMessage(author = user("steve", STEVE_ID), member = member(null), isWebhook = false)

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "steve", authorId = STEVE_ID, text = "hello", target = REPLIED_REF), reply)
    }

    @Test
    fun GIVEN_reply_to_message_bridge_relayed_WHEN_mapped_THEN_reply_names_player_without_source_tag() = runTest {
        val replied = repliedMessage(author = user("[MC] Steve", WEBHOOK_ID), member = null, isWebhook = true)

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "Steve", authorId = null, text = "hello", target = null), reply)
    }

    @Test
    fun GIVEN_reply_to_relayed_copy_still_remembered_WHEN_mapped_THEN_reply_carries_the_original_message() = runTest {
        relayedMessageCache.remember(messageId = REPLIED_ID, text = relayedTelegramMessage)
        val replied = repliedMessage(
            author = user("[TG] steve_tg", WEBHOOK_ID),
            member = null,
            isWebhook = true,
            content = "-# ↪ Alex: hi\nhello from telegram"
        )

        val reply = mapper.map(replyTo(replied))

        assertEquals(
            Text.Reply(
                author = "steve_tg",
                authorId = null,
                text = "hello from telegram",
                target = relayedTelegramMessage.ref
            ),
            reply
        )
    }

    @Test
    fun GIVEN_reply_to_relayed_copy_with_reply_header_WHEN_no_longer_remembered_THEN_header_is_dropped() = runTest {
        val replied = repliedMessage(
            author = user("[MC] Steve", WEBHOOK_ID),
            member = null,
            isWebhook = true,
            content = "-# ↪ Alex: hi\nhello\nagain"
        )

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "Steve", authorId = null, text = "hello\nagain", target = null), reply)
    }

    @Test
    fun GIVEN_reply_to_webhook_of_another_app_WHEN_mapped_THEN_reply_keeps_its_name() = runTest {
        val replied = repliedMessage(author = user("[CI] GitHub", WEBHOOK_ID), member = null, isWebhook = true)

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "[CI] GitHub", authorId = null, text = "hello", target = null), reply)
    }

    @Test
    fun GIVEN_reply_to_join_embed_of_the_bridge_WHEN_mapped_THEN_reply_is_labelled_as_the_server() = runTest {
        val joined = EmbedBuilder().setAuthor("Steve joined").build()
        val replied = repliedMessage(
            author = user("MessageBridge", BOT_ID),
            member = null,
            isWebhook = false,
            content = "",
            embeds = listOf(joined)
        )

        val reply = mapper.map(replyTo(replied))

        assertEquals(Text.Reply(author = "[server]", authorId = null, text = "Steve joined", target = null), reply)
    }

    @Test
    fun GIVEN_reply_to_plain_message_of_the_bridge_WHEN_mapped_THEN_reply_quotes_it_as_the_server() = runTest {
        val replied = repliedMessage(
            author = user("MessageBridge", BOT_ID),
            member = null,
            isWebhook = false,
            content = "The server has started"
        )

        val reply = mapper.map(replyTo(replied))

        assertEquals(
            Text.Reply(author = "[server]", authorId = null, text = "The server has started", target = null),
            reply
        )
    }

    @Test
    fun GIVEN_reply_to_message_of_another_bot_WHEN_mapped_THEN_reply_names_that_bot() = runTest {
        val replied = repliedMessage(author = user("Helper", OTHER_BOT_ID), member = null, isWebhook = false)

        val reply = mapper.map(replyTo(replied))

        assertEquals(
            Text.Reply(author = "Helper", authorId = OTHER_BOT_ID, text = "hello", target = REPLIED_REF),
            reply
        )
    }

    private companion object {
        const val REPLIED_ID = 500L
        val REPLIED_REF = MessageRef.Discord(messageId = REPLIED_ID)
        const val STEVE_ID = 42L
        const val WEBHOOK_ID = 77L
        const val BOT_ID = 99L
        const val OTHER_BOT_ID = 98L
    }
}
