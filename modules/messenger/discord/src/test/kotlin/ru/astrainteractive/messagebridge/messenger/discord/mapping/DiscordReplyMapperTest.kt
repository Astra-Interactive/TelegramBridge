@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.mapping

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.entities.User
import ru.astrainteractive.messagebridge.messaging.model.Text
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscordReplyMapperTest {
    private val mapper = DiscordReplyMapper()

    private inline fun <reified T : Any> jdaFake(answerByGetter: Map<String, Any?>): T {
        val fake = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
            check(method.name in answerByGetter) { "${T::class.simpleName}.${method.name} is not faked" }
            answerByGetter[method.name]
        }
        return fake as T
    }

    private fun user(name: String, id: Long): User = jdaFake(mapOf("getName" to name, "getIdLong" to id))

    private fun member(nickname: String?): Member = jdaFake(mapOf("getNickname" to nickname))

    private fun repliedMessage(author: User, member: Member?, isWebhook: Boolean): Message = jdaFake(
        mapOf(
            "getAuthor" to author,
            "getMember" to member,
            "isWebhookMessage" to isWebhook,
            "getContentRaw" to "hello"
        )
    )

    private fun message(type: MessageType, referenced: Message?): Message = jdaFake(
        mapOf(
            "getType" to type,
            "getReferencedMessage" to referenced
        )
    )

    @Test
    fun GIVEN_message_that_is_not_a_reply_WHEN_mapped_THEN_returns_null() {
        assertNull(mapper.map(message(type = MessageType.DEFAULT, referenced = null)))
    }

    @Test
    fun GIVEN_reply_whose_original_discord_did_not_send_WHEN_mapped_THEN_returns_null() {
        assertNull(mapper.map(message(type = MessageType.INLINE_REPLY, referenced = null)))
    }

    @Test
    fun GIVEN_reply_to_member_with_nickname_WHEN_mapped_THEN_reply_names_nickname() {
        val replied = repliedMessage(author = user("steve", STEVE_ID), member = member("Stevie"), isWebhook = false)

        val reply = mapper.map(message(type = MessageType.INLINE_REPLY, referenced = replied))

        assertEquals(Text.Reply(author = "Stevie", authorId = STEVE_ID, text = "hello"), reply)
    }

    @Test
    fun GIVEN_reply_to_member_without_nickname_WHEN_mapped_THEN_reply_names_user() {
        val replied = repliedMessage(author = user("steve", STEVE_ID), member = member(null), isWebhook = false)

        val reply = mapper.map(message(type = MessageType.INLINE_REPLY, referenced = replied))

        assertEquals(Text.Reply(author = "steve", authorId = STEVE_ID, text = "hello"), reply)
    }

    @Test
    fun GIVEN_reply_to_message_bridge_relayed_WHEN_mapped_THEN_reply_names_player_without_source_tag() {
        val replied = repliedMessage(author = user("[MC] Steve", WEBHOOK_ID), member = null, isWebhook = true)

        val reply = mapper.map(message(type = MessageType.INLINE_REPLY, referenced = replied))

        assertEquals(Text.Reply(author = "Steve", authorId = null, text = "hello"), reply)
    }

    @Test
    fun GIVEN_reply_to_webhook_of_another_app_WHEN_mapped_THEN_reply_keeps_its_name() {
        val replied = repliedMessage(author = user("[CI] GitHub", WEBHOOK_ID), member = null, isWebhook = true)

        val reply = mapper.map(message(type = MessageType.INLINE_REPLY, referenced = replied))

        assertEquals(Text.Reply(author = "[CI] GitHub", authorId = null, text = "hello"), reply)
    }

    private companion object {
        const val STEVE_ID = 42L
        const val WEBHOOK_ID = 77L
    }
}
