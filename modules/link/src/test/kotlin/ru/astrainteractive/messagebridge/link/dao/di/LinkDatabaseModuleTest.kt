@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.dao.di

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.link.dao.model.LinkedPlayerStorageError
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LinkDatabaseModuleTest {
    private val dataFolder: File = createTempDirectory("link-database-test").toFile()
    private val steveUuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002")

    private fun blockedFolder(): File {
        return dataFolder.resolve("blocked").also { blocker -> blocker.writeText("") }
    }

    private fun File.free() {
        delete()
        mkdirs()
    }

    private fun TestScope.startModule(dataFolder: File): LinkDatabaseModule {
        val module = LinkDatabaseModule(ioScope = backgroundScope, dataFolder = dataFolder)
        runCurrent()
        return module
    }

    private fun TestScope.failNextAttemptAfter(retryDelay: Duration) {
        advanceTimeBy(retryDelay)
        runCurrent()
    }

    @AfterTest
    fun deleteDatabase() {
        dataFolder.deleteRecursively()
    }

    @Test
    fun GIVEN_folder_that_can_not_hold_the_database_WHEN_player_is_looked_up_THEN_fails_without_waiting() = runTest {
        val module = startModule(blockedFolder())

        val result = module.linkingDao.findByUuid(steveUuid)

        assertIs<LinkedPlayerStorageError>(result.exceptionOrNull())
        assertEquals(0L, currentTime)
    }

    @Test
    fun GIVEN_blocked_folder_WHEN_freed_THEN_database_opens_one_second_after_the_first_failure() = runTest {
        val blocked = blockedFolder()
        val module = startModule(blocked)
        blocked.free()

        advanceTimeBy(1.seconds)
        assertIs<LinkedPlayerStorageError>(module.linkingDao.findByUuid(steveUuid).exceptionOrNull())
        runCurrent()

        assertNull(module.linkingDao.findByUuid(steveUuid).getOrThrow())
    }

    @Test
    fun GIVEN_three_failed_attempts_WHEN_folder_is_freed_THEN_fourth_attempt_comes_four_seconds_later() = runTest {
        val blocked = blockedFolder()
        val module = startModule(blocked)
        failNextAttemptAfter(1.seconds)
        failNextAttemptAfter(2.seconds)
        blocked.free()

        advanceTimeBy(4.seconds)
        assertIs<LinkedPlayerStorageError>(module.linkingDao.findByUuid(steveUuid).exceptionOrNull())
        runCurrent()

        assertNull(module.linkingDao.findByUuid(steveUuid).getOrThrow())
        assertEquals(7.seconds, currentTime.milliseconds)
    }

    @Test
    fun GIVEN_seven_failed_attempts_WHEN_folder_is_freed_THEN_next_attempt_waits_a_minute_not_longer() = runTest {
        val blocked = blockedFolder()
        val module = startModule(blocked)
        listOf(1, 2, 4, 8, 16, 32).forEach { seconds -> failNextAttemptAfter(seconds.seconds) }
        blocked.free()

        advanceTimeBy(60.seconds)
        assertIs<LinkedPlayerStorageError>(module.linkingDao.findByUuid(steveUuid).exceptionOrNull())
        runCurrent()

        assertNull(module.linkingDao.findByUuid(steveUuid).getOrThrow())
        assertEquals(123.seconds, currentTime.milliseconds)
    }
}
