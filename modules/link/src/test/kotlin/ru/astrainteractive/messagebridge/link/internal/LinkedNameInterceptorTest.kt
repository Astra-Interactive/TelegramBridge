@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.internal

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.messaging.model.Text
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LinkedNameInterceptorTest {
    private val linkingDao = FakeLinkingDao()
    private val interceptor = LinkedNameInterceptor(linkingDao)

    private suspend fun link(name: String, uuid: String, discordId: Long, telegramId: Long) {
        linkingDao.upsert(
            LinkedPlayerModel(
                uuid = UUID.fromString(uuid),
                lastMinecraftName = name,
                discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = name, discordId = discordId),
                telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = name, telegramId = telegramId)
            )
        )
    }

    private fun telegram(author: String, authorId: Long, reply: Text.Reply?): Text.Telegram {
        return Text.Telegram(author = author, text = "hi", authorId = authorId, reply = reply)
    }

    private fun discord(author: String, authorId: Long, reply: Text.Reply?): Text.Discord {
        return Text.Discord(author = author, text = "hi", authorId = authorId, reply = reply)
    }

    private suspend fun linkSteveAndAlex() {
        link(name = "Steve", uuid = "5e4a7f7a-0000-4000-8000-000000000002", discordId = STEVE_DS, telegramId = STEVE_TG)
        link(name = "Alex", uuid = "5e4a7f7a-0000-4000-8000-000000000003", discordId = ALEX_DS, telegramId = ALEX_TG)
    }

    @Test
    fun GIVEN_linked_telegram_author_WHEN_intercepted_THEN_author_is_their_minecraft_name() = runTest {
        linkSteveAndAlex()

        val text = interceptor.intercept(telegram(author = "s_tg", authorId = STEVE_TG, reply = null))

        assertEquals(telegram(author = "Steve", authorId = STEVE_TG, reply = null), text)
    }

    @Test
    fun GIVEN_linked_discord_author_WHEN_intercepted_THEN_author_is_their_minecraft_name() = runTest {
        linkSteveAndAlex()

        val text = interceptor.intercept(discord(author = "Stevie", authorId = STEVE_DS, reply = null))

        assertEquals(discord(author = "Steve", authorId = STEVE_DS, reply = null), text)
    }

    @Test
    fun GIVEN_author_who_never_linked_WHEN_intercepted_THEN_text_keeps_their_name() = runTest {
        linkSteveAndAlex()
        val original = telegram(author = "bob_tg", authorId = BOB_TG, reply = null)

        assertEquals(original, interceptor.intercept(original))
    }

    @Test
    fun GIVEN_telegram_reply_to_linked_author_WHEN_intercepted_THEN_reply_names_their_minecraft_name() = runTest {
        linkSteveAndAlex()
        val reply = Text.Reply(author = "a_tg", authorId = ALEX_TG, text = "hello")

        val text = interceptor.intercept(telegram(author = "s_tg", authorId = STEVE_TG, reply = reply))

        assertEquals(reply.copy(author = "Alex"), text.reply)
    }

    @Test
    fun GIVEN_discord_reply_to_linked_author_WHEN_intercepted_THEN_reply_names_their_minecraft_name() = runTest {
        linkSteveAndAlex()
        val reply = Text.Reply(author = "Alexa", authorId = ALEX_DS, text = "hello")

        val text = interceptor.intercept(discord(author = "Stevie", authorId = STEVE_DS, reply = reply))

        assertEquals(reply.copy(author = "Alex"), text.reply)
    }

    @Test
    fun GIVEN_reply_without_author_id_WHEN_intercepted_THEN_reply_is_unchanged() = runTest {
        linkSteveAndAlex()
        val reply = Text.Reply(author = "Alex", authorId = null, text = "relayed from the game")

        val text = interceptor.intercept(telegram(author = "s_tg", authorId = STEVE_TG, reply = reply))

        assertEquals(reply, text.reply)
    }

    @Test
    fun GIVEN_reply_to_author_who_never_linked_WHEN_intercepted_THEN_reply_is_unchanged() = runTest {
        linkSteveAndAlex()
        val reply = Text.Reply(author = "bob_tg", authorId = BOB_TG, text = "hello")

        val text = interceptor.intercept(telegram(author = "s_tg", authorId = STEVE_TG, reply = reply))

        assertEquals(reply, text.reply)
    }

    @Test
    fun GIVEN_unreadable_database_WHEN_intercepted_THEN_names_are_unchanged() = runTest {
        linkSteveAndAlex()
        linkingDao.findFailure = IllegalStateException("Database is locked")
        val original = telegram(
            author = "s_tg",
            authorId = STEVE_TG,
            reply = Text.Reply(author = "a_tg", authorId = ALEX_TG, text = "hello")
        )

        assertEquals(original, interceptor.intercept(original))
    }

    @Test
    fun GIVEN_message_from_the_game_WHEN_intercepted_THEN_same_text_is_returned() = runTest {
        linkSteveAndAlex()
        val original = Text.Minecraft(author = "Steve", uuid = "5e4a7f7a-0000-4000-8000-000000000002", text = "hi")

        assertSame(original, interceptor.intercept(original))
    }

    private companion object {
        const val STEVE_DS = 4242L
        const val STEVE_TG = 77L
        const val ALEX_DS = 4343L
        const val ALEX_TG = 78L
        const val BOB_TG = 79L
    }
}
