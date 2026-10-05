@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.dao.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.messagebridge.link.dao.api.LinkingDao
import ru.astrainteractive.messagebridge.link.dao.di.LinkDatabaseModule
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayerStorageError
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class LinkingDaoImplTest {
    private val dataFolder: File = createTempDirectory("link-dao-test").toFile()
    private val steveUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")
    private val alexUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003")
    private val stevie = MessengerAccount.Discord(id = STEVE_DISCORD_ID, name = "Stevie")
    private val steveTelegram = MessengerAccount.Telegram(id = STEVE_TELEGRAM_ID, username = "steve_tg")

    private fun TestScope.dao(): LinkingDao {
        val module = LinkDatabaseModule(ioScope = backgroundScope, dataFolder = dataFolder)
        runCurrent()
        return module.linkingDao
    }

    @AfterTest
    fun deleteDatabase() {
        dataFolder.deleteRecursively()
    }

    @Test
    fun GIVEN_linked_discord_account_WHEN_found_by_its_id_THEN_returns_the_player_with_that_account() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        val player = dao.findByDiscordId(STEVE_DISCORD_ID).getOrThrow()

        assertEquals(LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = stevie, telegram = null), player)
    }

    @Test
    fun GIVEN_player_with_discord_WHEN_telegram_is_linked_THEN_both_accounts_are_kept() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        dao.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram).getOrThrow()

        val expected = LinkedPlayer(steveUuid, minecraftName = "Steve", discord = stevie, telegram = steveTelegram)
        assertEquals(expected, dao.findByUuid(steveUuid).getOrThrow())
        assertEquals(expected, dao.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow())
    }

    @Test
    fun GIVEN_player_who_changed_name_WHEN_links_another_account_THEN_new_name_is_stored() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        dao.link(uuid = steveUuid, minecraftName = "Steven", account = steveTelegram).getOrThrow()

        assertEquals("Steven", dao.findByUuid(steveUuid).getOrThrow()?.minecraftName)
    }

    @Test
    fun GIVEN_discord_account_of_another_player_WHEN_linked_again_THEN_fails_at_once_and_owner_keeps_it() = runTest {
        val dao = dao()
        dao.link(uuid = alexUuid, minecraftName = "Alex", account = stevie)
        val start = TimeSource.Monotonic.markNow()

        val result = dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        assertIs<LinkedPlayerStorageError>(result.exceptionOrNull())
        assertTrue(start.elapsedNow() < QUICK_FAILURE, "took ${start.elapsedNow()}")
        assertEquals(alexUuid, dao.findByDiscordId(STEVE_DISCORD_ID).getOrThrow()?.uuid)
        assertNull(dao.findByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_telegram_account_of_another_player_WHEN_linked_again_THEN_fails_at_once_and_owner_keeps_it() = runTest {
        val dao = dao()
        dao.link(uuid = alexUuid, minecraftName = "Alex", account = steveTelegram)
        val start = TimeSource.Monotonic.markNow()

        val result = dao.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

        assertIs<LinkedPlayerStorageError>(result.exceptionOrNull())
        assertTrue(start.elapsedNow() < QUICK_FAILURE, "took ${start.elapsedNow()}")
        assertEquals(alexUuid, dao.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow()?.uuid)
        assertNull(dao.findByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_player_with_a_discord_account_WHEN_another_discord_account_is_linked_THEN_fails() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        val otherAccount = MessengerAccount.Discord(id = OTHER_ID, name = "Steve2")

        val result = dao.link(uuid = steveUuid, minecraftName = "Steve", account = otherAccount)

        assertIs<LinkedPlayerStorageError>(result.exceptionOrNull())
        assertEquals(stevie, dao.findByUuid(steveUuid).getOrThrow()?.discord)
    }

    @Test
    fun GIVEN_nothing_linked_WHEN_unknown_ids_are_looked_up_THEN_success_without_a_player() = runTest {
        val dao = dao()

        assertNull(dao.findByUuid(steveUuid).getOrThrow())
        assertNull(dao.findByDiscordId(STEVE_DISCORD_ID).getOrThrow())
        assertNull(dao.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow())
    }

    @Test
    fun GIVEN_player_with_two_accounts_WHEN_deleted_THEN_returns_that_player_and_both_accounts_are_free() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

        val deleted = dao.deleteByUuid(steveUuid).getOrThrow()

        assertEquals(LinkedPlayer(steveUuid, "Steve", discord = stevie, telegram = steveTelegram), deleted)
        assertNull(dao.findByDiscordId(STEVE_DISCORD_ID).getOrThrow())
        assertNull(dao.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow())
        assertTrue(dao.link(uuid = alexUuid, minecraftName = "Alex", account = stevie).isSuccess)
    }

    @Test
    fun GIVEN_player_with_two_accounts_WHEN_discord_is_unlinked_THEN_telegram_stays_and_discord_is_free() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

        val unlinked = dao.unlinkDiscord(STEVE_DISCORD_ID).getOrThrow()

        assertEquals(stevie, unlinked?.discord)
        assertEquals(
            LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = null, telegram = steveTelegram),
            dao.findByUuid(steveUuid).getOrThrow()
        )
        assertNull(dao.findByDiscordId(STEVE_DISCORD_ID).getOrThrow())
    }

    @Test
    fun GIVEN_player_with_only_discord_WHEN_discord_is_unlinked_THEN_the_player_is_gone() = runTest {
        val dao = dao()
        dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        dao.unlinkDiscord(STEVE_DISCORD_ID).getOrThrow()

        assertNull(dao.findByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_unknown_discord_account_WHEN_unlinked_THEN_no_player_is_returned() = runTest {
        assertNull(dao().unlinkDiscord(STEVE_DISCORD_ID).getOrThrow())
    }

    @Test
    fun GIVEN_nothing_linked_WHEN_deleted_THEN_no_player_is_returned() = runTest {
        assertNull(dao().deleteByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_simultaneous_discord_and_telegram_links_WHEN_both_finish_THEN_both_accounts_are_stored() = runTest {
        val dao = dao()
        dao.findByUuid(steveUuid).getOrThrow()

        val results = withContext(Dispatchers.Default) {
            listOf(
                async { dao.link(uuid = steveUuid, minecraftName = "Steve", account = stevie) },
                async { dao.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram) }
            ).awaitAll()
        }

        assertTrue(results.all { result -> result.isSuccess }, "$results")
        assertEquals(
            LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = stevie, telegram = steveTelegram),
            dao.findByUuid(steveUuid).getOrThrow()
        )
    }

    @Test
    fun GIVEN_database_that_is_not_open_WHEN_player_is_looked_up_THEN_fails_at_once() = runTest {
        val dao = LinkingDaoImpl(MutableStateFlow(null))

        val result = dao.findByUuid(steveUuid)

        assertIs<LinkedPlayerStorageError>(result.exceptionOrNull())
        assertEquals(0L, currentTime)
    }

    private companion object {
        const val STEVE_DISCORD_ID = 4242L
        const val STEVE_TELEGRAM_ID = 77L
        const val OTHER_ID = 4343L
        val QUICK_FAILURE = 2.seconds
    }
}
