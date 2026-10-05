@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.config.PluginTranslation
import ru.astrainteractive.messagebridge.link.dao.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.dao.model.MessengerAccount
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.fake.RecordingOnlineKPlayer
import ru.astrainteractive.messagebridge.link.internal.DiscordRoleController
import ru.astrainteractive.messagebridge.link.internal.LuckPermsRoleController
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRoleChange
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnlinkCommandExecutorTest {
    private val translation = PluginTranslation()
    private val linkingDao = FakeLinkingDao()
    private val luckPermsProvider = FakeLuckPermsProvider()
    private val pluginConfiguration = PluginConfiguration(
        link = PluginConfiguration.Link(
            linkDiscordRole = "123456789012345678",
            linkLuckPermsRole = "verified"
        )
    )
    private val roleChanges = Channel<DiscordRoleChange>(Channel.UNLIMITED)
    private val executor = UnlinkCommandExecutor(
        linkingDao = linkingDao,
        luckPermsRoleController = LuckPermsRoleController(
            configKrate = DefaultMutableKrate(factory = { pluginConfiguration }, loader = { null }).asCachedKrate(),
            luckPermsProvider = luckPermsProvider
        ),
        discordRoleController = DiscordRoleController(
            configKrate = DefaultMutableKrate(factory = { pluginConfiguration }, loader = { null }).asCachedKrate(),
            roleChanges = roleChanges
        ),
        translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    )
    private val stevieRevoke = DiscordRoleChange.Revoke(discordUserId = 42, roleId = 123456789012345678)
    private val admin = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001"),
        name = "Admin"
    )
    private val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve"
    )

    private suspend fun linkSteve() {
        linkingDao.link(
            uuid = steve.uuid,
            minecraftName = "Steve",
            account = MessengerAccount.Discord(id = 42, name = "Stevie")
        )
    }

    private fun assertReadOnly(player: RecordingOnlineKPlayer, message: LocalizableComponent) {
        assertEquals(listOf(message), player.messages)
    }

    @Test
    fun GIVEN_linked_player_WHEN_unlinks_THEN_link_is_removed_and_player_reads_success() = runTest {
        linkSteve()

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertTrue(linkingDao.linkedPlayers.isEmpty())
        assertReadOnly(steve, translation.unlink.success)
    }

    @Test
    fun GIVEN_linked_player_WHEN_unlinks_THEN_luckperms_role_is_revoked() = runTest {
        linkSteve()

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_player_without_link_WHEN_unlinks_THEN_player_reads_not_linked() = runTest {
        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertReadOnly(steve, translation.unlink.notLinked)
    }

    @Test
    fun GIVEN_unreadable_database_WHEN_player_unlinks_THEN_link_stays_and_player_reads_unknown_error() = runTest {
        linkSteve()
        linkingDao.deleteFailure = IllegalStateException("Database is locked")

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertEquals(setOf(steve.uuid), linkingDao.linkedPlayers.keys)
        assertReadOnly(steve, translation.commandError.unknownError)
    }

    @Test
    fun GIVEN_database_that_can_not_delete_WHEN_player_unlinks_THEN_player_reads_unknown_error() = runTest {
        linkSteve()
        linkingDao.deleteFailure = IllegalStateException("Database is read-only")

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertReadOnly(steve, translation.commandError.unknownError)
        assertEquals(0, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_linked_player_WHEN_admin_unlinks_them_THEN_link_is_removed_and_admin_reads_player_success() = runTest {
        linkSteve()

        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertTrue(linkingDao.linkedPlayers.isEmpty())
        assertReadOnly(admin, translation.unlink.playerSuccess)
        assertTrue(steve.messages.isEmpty())
    }

    @Test
    fun GIVEN_player_without_link_WHEN_unlinks_THEN_their_luckperms_group_is_still_revoked() = runTest {
        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_player_without_link_WHEN_admin_unlinks_them_THEN_their_luckperms_group_is_still_revoked() = runTest {
        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertEquals(1, luckPermsProvider.provideCallCount)
    }

    @Test
    fun GIVEN_player_without_link_WHEN_admin_unlinks_them_THEN_admin_reads_player_not_linked() = runTest {
        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertReadOnly(admin, translation.unlink.playerNotLinked)
    }

    @Test
    fun GIVEN_unreadable_database_WHEN_admin_unlinks_a_player_THEN_admin_reads_unknown_error() = runTest {
        linkingDao.deleteFailure = IllegalStateException("Database is locked")

        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertReadOnly(admin, translation.commandError.unknownError)
    }

    @Test
    fun GIVEN_player_linked_to_discord_WHEN_unlinks_THEN_their_discord_role_is_revoked() = runTest {
        linkSteve()

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertEquals(stevieRevoke, roleChanges.tryReceive().getOrNull())
    }

    @Test
    fun GIVEN_player_linked_to_discord_WHEN_admin_unlinks_them_THEN_their_discord_role_is_revoked() = runTest {
        linkSteve()

        executor.onIntent(UnlinkCommandExecutor.Intent.AdminUnlink(targetPlayerUuid = steve.uuid, sender = admin))

        assertEquals(stevieRevoke, roleChanges.tryReceive().getOrNull())
    }

    @Test
    fun GIVEN_player_linked_only_to_telegram_WHEN_unlinks_THEN_no_discord_role_change_is_sent() = runTest {
        linkingDao.link(
            uuid = steve.uuid,
            minecraftName = "Steve",
            account = MessengerAccount.Telegram(id = 77, username = "steve_tg")
        )

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_player_without_link_WHEN_unlinks_THEN_no_discord_role_change_is_sent() = runTest {
        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertTrue(roleChanges.tryReceive().isFailure)
    }

    @Test
    fun GIVEN_database_that_can_not_delete_WHEN_unlinks_THEN_no_discord_role_change_is_sent() = runTest {
        linkSteve()
        linkingDao.deleteFailure = IllegalStateException("Database is read-only")

        executor.onIntent(UnlinkCommandExecutor.Intent.Unlink(steve))

        assertTrue(roleChanges.tryReceive().isFailure)
    }
}
