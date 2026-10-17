@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.exposed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import ru.astrainteractive.messagebridge.link.api.LinkedPlayerRepository
import ru.astrainteractive.messagebridge.link.di.LinkDatabaseModule
import ru.astrainteractive.messagebridge.link.model.LinkOutcome
import ru.astrainteractive.messagebridge.link.model.LinkedPlayer
import ru.astrainteractive.messagebridge.link.model.LinkedPlayerStorageError
import ru.astrainteractive.messagebridge.link.model.MessengerAccount
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

class ExposedLinkedPlayerRepositoryTest {
    private val dataFolder: File = createTempDirectory("link-repository-test").toFile()
    private val steveUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")
    private val alexUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000003")
    private val stevie = MessengerAccount.Discord(id = STEVE_DISCORD_ID, name = "Stevie")
    private val steveTelegram = MessengerAccount.Telegram(id = STEVE_TELEGRAM_ID, username = "steve_tg")

    private fun TestScope.repository(): LinkedPlayerRepository {
        val module = LinkDatabaseModule(ioScope = backgroundScope, dataFolder = dataFolder)
        runCurrent()
        return module.linkedPlayerRepository
    }

    @AfterTest
    fun deleteDatabase() {
        dataFolder.deleteRecursively()
    }

    @Test
    fun GIVEN_linked_discord_account_WHEN_found_by_its_id_THEN_returns_the_player_with_that_account() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        val player = repository.findByDiscordId(STEVE_DISCORD_ID).getOrThrow()

        assertEquals(LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = stevie, telegram = null), player)
    }

    @Test
    fun GIVEN_player_with_discord_WHEN_telegram_is_linked_THEN_both_accounts_are_kept() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        repository.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram).getOrThrow()

        val expected = LinkedPlayer(steveUuid, minecraftName = "Steve", discord = stevie, telegram = steveTelegram)
        assertEquals(expected, repository.findByUuid(steveUuid).getOrThrow())
        assertEquals(expected, repository.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow())
    }

    @Test
    fun GIVEN_player_who_changed_name_WHEN_links_another_account_THEN_new_name_is_stored() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        repository.link(uuid = steveUuid, minecraftName = "Steven", account = steveTelegram).getOrThrow()

        assertEquals("Steven", repository.findByUuid(steveUuid).getOrThrow()?.minecraftName)
    }

    @Test
    fun GIVEN_discord_account_of_another_player_WHEN_linked_again_THEN_account_taken_at_once_and_owner_keeps_it() =
        runTest {
            val repository = repository()
            repository.link(uuid = alexUuid, minecraftName = "Alex", account = stevie)
            val start = TimeSource.Monotonic.markNow()

            val outcome = repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie).getOrThrow()

            assertEquals(LinkOutcome.AccountTaken, outcome)
            assertTrue(start.elapsedNow() < QUICK_FAILURE, "took ${start.elapsedNow()}")
            assertEquals(alexUuid, repository.findByDiscordId(STEVE_DISCORD_ID).getOrThrow()?.uuid)
            assertNull(repository.findByUuid(steveUuid).getOrThrow())
        }

    @Test
    fun GIVEN_telegram_account_of_another_player_WHEN_linked_again_THEN_account_taken_at_once_and_owner_keeps_it() =
        runTest {
            val repository = repository()
            repository.link(uuid = alexUuid, minecraftName = "Alex", account = steveTelegram)
            val start = TimeSource.Monotonic.markNow()

            val outcome = repository.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram).getOrThrow()

            assertEquals(LinkOutcome.AccountTaken, outcome)
            assertTrue(start.elapsedNow() < QUICK_FAILURE, "took ${start.elapsedNow()}")
            assertEquals(alexUuid, repository.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow()?.uuid)
            assertNull(repository.findByUuid(steveUuid).getOrThrow())
        }

    @Test
    fun GIVEN_player_with_a_discord_account_WHEN_another_discord_account_is_linked_THEN_already_linked() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        val otherAccount = MessengerAccount.Discord(id = OTHER_ID, name = "Steve2")

        val outcome = repository.link(uuid = steveUuid, minecraftName = "Steve", account = otherAccount).getOrThrow()

        assertEquals(LinkOutcome.AlreadyLinked, outcome)
        assertEquals(stevie, repository.findByUuid(steveUuid).getOrThrow()?.discord)
    }

    @Test
    fun GIVEN_linked_discord_account_WHEN_its_player_links_it_again_THEN_already_linked() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        val outcome = repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie).getOrThrow()

        assertEquals(LinkOutcome.AlreadyLinked, outcome)
    }

    @Test
    fun GIVEN_two_players_WHEN_both_link_the_same_account_at_once_THEN_one_is_linked_and_the_other_reads_taken() =
        runTest {
            val repository = repository()
            repository.findByUuid(steveUuid).getOrThrow()

            val outcomes = withContext(Dispatchers.Default) {
                listOf(
                    async { repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie) },
                    async { repository.link(uuid = alexUuid, minecraftName = "Alex", account = stevie) }
                ).awaitAll()
            }.map { result -> result.getOrThrow() }

            assertEquals(setOf(LinkOutcome.Linked, LinkOutcome.AccountTaken), outcomes.toSet())
            val linkedPlayers = listOfNotNull(
                repository.findByUuid(steveUuid).getOrThrow(),
                repository.findByUuid(alexUuid).getOrThrow()
            )
            assertEquals(listOf(stevie), linkedPlayers.map { player -> player.discord })
        }

    @Test
    fun GIVEN_nothing_linked_WHEN_unknown_ids_are_looked_up_THEN_success_without_a_player() = runTest {
        val repository = repository()

        assertNull(repository.findByUuid(steveUuid).getOrThrow())
        assertNull(repository.findByDiscordId(STEVE_DISCORD_ID).getOrThrow())
        assertNull(repository.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow())
    }

    @Test
    fun GIVEN_player_with_two_accounts_WHEN_deleted_THEN_returns_that_player_and_both_accounts_are_free() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

        val deleted = repository.deleteByUuid(steveUuid).getOrThrow()

        assertEquals(LinkedPlayer(steveUuid, "Steve", discord = stevie, telegram = steveTelegram), deleted)
        assertNull(repository.findByDiscordId(STEVE_DISCORD_ID).getOrThrow())
        assertNull(repository.findByTelegramId(STEVE_TELEGRAM_ID).getOrThrow())
        val relinked = repository.link(uuid = alexUuid, minecraftName = "Alex", account = stevie).getOrThrow()
        assertEquals(LinkOutcome.Linked, relinked)
    }

    @Test
    fun GIVEN_player_with_two_accounts_WHEN_discord_is_unlinked_THEN_telegram_stays_and_discord_is_free() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram)

        val unlinked = repository.unlinkDiscord(STEVE_DISCORD_ID).getOrThrow()

        assertEquals(stevie, unlinked?.discord)
        assertEquals(
            LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = null, telegram = steveTelegram),
            repository.findByUuid(steveUuid).getOrThrow()
        )
        assertNull(repository.findByDiscordId(STEVE_DISCORD_ID).getOrThrow())
    }

    @Test
    fun GIVEN_player_with_only_discord_WHEN_discord_is_unlinked_THEN_the_player_is_gone() = runTest {
        val repository = repository()
        repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie)

        repository.unlinkDiscord(STEVE_DISCORD_ID).getOrThrow()

        assertNull(repository.findByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_unknown_discord_account_WHEN_unlinked_THEN_no_player_is_returned() = runTest {
        assertNull(repository().unlinkDiscord(STEVE_DISCORD_ID).getOrThrow())
    }

    @Test
    fun GIVEN_nothing_linked_WHEN_deleted_THEN_no_player_is_returned() = runTest {
        assertNull(repository().deleteByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_simultaneous_discord_and_telegram_links_WHEN_both_finish_THEN_both_accounts_are_stored() = runTest {
        val repository = repository()
        repository.findByUuid(steveUuid).getOrThrow()

        val results = withContext(Dispatchers.Default) {
            listOf(
                async { repository.link(uuid = steveUuid, minecraftName = "Steve", account = stevie) },
                async { repository.link(uuid = steveUuid, minecraftName = "Steve", account = steveTelegram) }
            ).awaitAll()
        }

        assertTrue(results.all { result -> result.isSuccess }, "$results")
        assertEquals(
            LinkedPlayer(uuid = steveUuid, minecraftName = "Steve", discord = stevie, telegram = steveTelegram),
            repository.findByUuid(steveUuid).getOrThrow()
        )
    }

    @Test
    fun GIVEN_database_that_is_not_open_WHEN_player_is_looked_up_THEN_fails_at_once() = runTest {
        val repository = ExposedLinkedPlayerRepository(MutableStateFlow(null))

        val result = repository.findByUuid(steveUuid)

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
