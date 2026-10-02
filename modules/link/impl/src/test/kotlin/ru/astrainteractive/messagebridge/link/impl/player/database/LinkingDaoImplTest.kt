@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.impl.player.database

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.astrainteractive.messagebridge.link.api.player.model.LinkedPlayerModel
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkingDaoImplTest {
    private val database = Database.connect(
        url = "jdbc:h2:mem:linking-${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver"
    ).also { database ->
        transaction(database) { SchemaUtils.create(MinecraftPlayerTable, DiscordLinkTable, TelegramLinkTable) }
    }
    private val dao = LinkingDaoImpl(flowOf(database))

    private val steve = LinkedPlayerModel(
        uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        lastMinecraftName = "Steve",
        discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "steve", discordId = 42L),
        telegramLink = null
    )
    private val alex = LinkedPlayerModel(
        uuid = UUID.fromString("ec561538-f3fd-461d-aff5-086b22154bce"),
        lastMinecraftName = "Alex",
        discordLink = null,
        telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = "alex_tg", telegramId = 7L)
    )

    @Test
    fun GIVEN_players_linked_on_discord_and_telegram_WHEN_discord_links_are_listed_THEN_only_discord_ones() = runTest {
        dao.upsert(steve).getOrThrow()
        dao.upsert(alex).getOrThrow()

        assertEquals(listOf(steve), dao.findAllWithDiscordLink().getOrThrow())
    }

    @Test
    fun GIVEN_discord_link_is_cleared_WHEN_discord_links_are_listed_THEN_the_player_is_not_there() = runTest {
        dao.upsert(steve).getOrThrow()
        dao.upsert(steve.copy(discordLink = null, telegramLink = alex.telegramLink)).getOrThrow()

        assertEquals(emptyList<LinkedPlayerModel>(), dao.findAllWithDiscordLink().getOrThrow())
        assertNull(dao.findByDiscordId(42L).getOrThrow())
    }

    @Test
    fun GIVEN_nobody_has_the_id_WHEN_found_by_it_THEN_nobody_is_found_without_a_failure() = runTest {
        dao.upsert(steve).getOrThrow()

        assertNull(dao.findByDiscordId(43L).getOrThrow())
        assertNull(dao.findByTelegramId(7L).getOrThrow())
    }

    @Test
    fun GIVEN_saved_player_WHEN_found_by_every_id_THEN_it_is_the_same_player() = runTest {
        dao.upsert(alex).getOrThrow()

        assertEquals(alex, dao.findByUuid(alex.uuid).getOrThrow())
        assertEquals(alex, dao.findByTelegramId(7L).getOrThrow())
    }

    @Test
    fun GIVEN_deleted_player_WHEN_found_THEN_nobody_is_found() = runTest {
        dao.upsert(steve).getOrThrow()

        dao.deleteByUuid(steve.uuid).getOrThrow()

        assertNull(dao.findByUuid(steve.uuid).getOrThrow())
    }

    @Test
    fun GIVEN_saved_player_WHEN_saved_again_with_new_names_THEN_the_new_names_are_found() = runTest {
        dao.upsert(steve).getOrThrow()
        val renamed = steve.copy(
            lastMinecraftName = "Steve2",
            discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "steve2", discordId = 42L)
        )

        dao.upsert(renamed).getOrThrow()

        assertEquals(renamed, dao.findByUuid(steve.uuid).getOrThrow())
    }

    @Test
    fun GIVEN_discord_account_of_another_player_WHEN_saved_THEN_it_fails_and_nothing_is_saved() = runTest {
        dao.upsert(steve).getOrThrow()

        val result = dao.upsert(alex.copy(discordLink = steve.discordLink))

        assertTrue(result.isFailure)
        assertNull(dao.findByUuid(alex.uuid).getOrThrow())
        assertEquals(steve, dao.findByDiscordId(42L).getOrThrow())
    }

    @Test
    fun GIVEN_deleted_player_with_both_links_WHEN_another_player_takes_the_accounts_THEN_it_is_saved() = runTest {
        dao.upsert(steve.copy(telegramLink = alex.telegramLink)).getOrThrow()
        dao.deleteByUuid(steve.uuid).getOrThrow()
        val newOwner = alex.copy(discordLink = steve.discordLink)

        dao.upsert(newOwner).getOrThrow()

        assertEquals(newOwner, dao.findByDiscordId(42L).getOrThrow())
        assertEquals(newOwner, dao.findByTelegramId(7L).getOrThrow())
    }
}
