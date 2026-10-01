@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.commands.unlink

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.commands.fake.FakeUnlinking
import ru.astrainteractive.messagebridge.commands.fake.RecordingOnlineKPlayer
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.link.UnlinkResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnlinkCommandExecutorTest {
    private val translation = PluginTranslation()
    private val unlinking = FakeUnlinking(response = UnlinkResponse.Unlinked)
    private val executor = UnlinkCommandExecutor(
        unlinking = unlinking,
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )
    private val admin = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001"),
        name = "Admin"
    )
    private val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve"
    )

    private fun assertReadOnly(player: RecordingOnlineKPlayer, message: LocalizableComponent) {
        assertEquals(listOf(message), player.messages)
    }

    @Test
    fun GIVEN_linked_player_WHEN_unlinks_THEN_their_links_are_removed_and_player_reads_success() = runTest {
        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertEquals(listOf(steve.uuid), unlinking.unlinkedPlayers)
        assertReadOnly(steve, translation.unlink.success)
    }

    @Test
    fun GIVEN_player_without_link_WHEN_unlinks_THEN_player_reads_not_linked() = runTest {
        unlinking.response = UnlinkResponse.NotLinked

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertReadOnly(steve, translation.unlink.notLinked)
    }

    @Test
    fun GIVEN_links_that_cannot_be_removed_WHEN_player_unlinks_THEN_player_reads_unknown_error() = runTest {
        unlinking.response = UnlinkResponse.UnknownError

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertReadOnly(steve, translation.commandError.unknownError)
    }

    @Test
    fun GIVEN_linked_player_WHEN_admin_unlinks_them_THEN_their_links_are_removed_and_admin_reads_player_success() =
        runTest {
            executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

            assertEquals(listOf(steve.uuid), unlinking.unlinkedPlayers)
            assertReadOnly(admin, translation.unlink.playerSuccess)
            assertTrue(steve.messages.isEmpty())
        }

    @Test
    fun GIVEN_player_without_link_WHEN_admin_unlinks_them_THEN_admin_reads_player_not_linked() = runTest {
        unlinking.response = UnlinkResponse.NotLinked

        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertReadOnly(admin, translation.unlink.playerNotLinked)
    }

    @Test
    fun GIVEN_links_that_cannot_be_removed_WHEN_admin_unlinks_a_player_THEN_admin_reads_unknown_error() = runTest {
        unlinking.response = UnlinkResponse.UnknownError

        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertReadOnly(admin, translation.commandError.unknownError)
    }
}
