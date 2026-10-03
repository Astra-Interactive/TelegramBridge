@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.internal

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.messaging.model.Text
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkedDiscordAuthorResolverTest {
    private val linkingDao = FakeLinkingDao()
    private val resolver = LinkedDiscordAuthorResolver(linkingDao)
    private val steveUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")
    private val discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "Stevie", discordId = DISCORD_ID)
    private val telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = "steve_tg", telegramId = TELEGRAM_ID)

    private suspend fun linkSteve(discordLink: LinkedPlayerModel.DiscordLink?) {
        linkingDao.upsert(
            LinkedPlayerModel(
                uuid = steveUuid,
                lastMinecraftName = "Steve",
                discordLink = discordLink,
                telegramLink = telegramLink
            )
        )
    }

    @Test
    fun GIVEN_player_linked_to_discord_WHEN_their_game_message_is_resolved_THEN_returns_their_discord_id() = runTest {
        linkSteve(discordLink)

        val discordId = resolver.discordUserId(Text.Minecraft(author = "Steve", uuid = "$steveUuid", text = "hi"))

        assertEquals(DISCORD_ID, discordId)
    }

    @Test
    fun GIVEN_telegram_author_linked_to_discord_WHEN_resolved_THEN_returns_discord_id() = runTest {
        linkSteve(discordLink)

        val discordId = resolver.discordUserId(
            Text.Telegram(author = "steve_tg", text = "hi", authorId = TELEGRAM_ID, reply = null)
        )

        assertEquals(DISCORD_ID, discordId)
    }

    @Test
    fun GIVEN_linked_discord_author_WHEN_resolved_THEN_returns_their_own_discord_id() = runTest {
        linkSteve(discordLink)

        val discordId = resolver.discordUserId(
            Text.Discord(author = "Stevie", text = "hi", authorId = DISCORD_ID, reply = null)
        )

        assertEquals(DISCORD_ID, discordId)
    }

    @Test
    fun GIVEN_telegram_author_linked_without_discord_WHEN_resolved_THEN_returns_null() = runTest {
        linkSteve(discordLink = null)

        val discordId = resolver.discordUserId(
            Text.Telegram(author = "steve_tg", text = "hi", authorId = TELEGRAM_ID, reply = null)
        )

        assertNull(discordId)
    }

    @Test
    fun GIVEN_author_who_never_linked_WHEN_resolved_THEN_returns_null() = runTest {
        val discordId = resolver.discordUserId(
            Text.Telegram(author = "alex_tg", text = "hi", authorId = OTHER_TELEGRAM_ID, reply = null)
        )

        assertNull(discordId)
    }

    @Test
    fun GIVEN_unreadable_database_WHEN_game_message_is_resolved_THEN_returns_null() = runTest {
        linkSteve(discordLink)
        linkingDao.findFailure = IllegalStateException("Database is locked")

        val discordId = resolver.discordUserId(Text.Minecraft(author = "Steve", uuid = "$steveUuid", text = "hi"))

        assertNull(discordId)
    }

    private companion object {
        const val DISCORD_ID = 4242L
        const val TELEGRAM_ID = 77L
        const val OTHER_TELEGRAM_ID = 78L
    }
}
