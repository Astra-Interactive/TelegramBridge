@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.link.controller.LuckPermsRoleController
import ru.astrainteractive.messagebridge.link.database.model.LinkedPlayerModel
import ru.astrainteractive.messagebridge.link.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.fake.FakeLuckPermsProvider
import ru.astrainteractive.messagebridge.link.fake.FakeMultiplatformCommands
import ru.astrainteractive.messagebridge.link.fake.FakePlatformServer
import ru.astrainteractive.messagebridge.link.fake.RecordingOnlineKPlayer
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnlinkLiteralArgumentBuilderTest {
    private val translation = PluginTranslation()
    private val translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    private val linkingDao = FakeLinkingDao()
    private val luckPermsRoleController = LuckPermsRoleController(
        configKrate = DefaultMutableKrate(factory = { PluginConfiguration() }, loader = { null }).asCachedKrate(),
        luckPermsProvider = FakeLuckPermsProvider()
    )
    private val admin = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001"),
        name = "Admin",
        permissions = setOf(PluginPermission.UnlinkPlayer)
    )
    private val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve"
    )

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private suspend fun linkSteve() {
        linkingDao.upsert(LinkedPlayerModel(uuid = steve.uuid, lastMinecraftName = "Steve"))
    }

    private fun execute(input: String, sender: RecordingOnlineKPlayer) {
        val multiplatformCommand = MultiplatformCommand(FakeMultiplatformCommands(sender))
        val builder = UnlinkLiteralArgumentBuilder(
            executor = UnlinkCommandExecutor(
                linkingDao = linkingDao,
                luckPermsRoleController = luckPermsRoleController,
                translationKrate = translationKrate
            ),
            ioScope = ioScope,
            multiplatformCommand = multiplatformCommand,
            platformServer = FakePlatformServer(onlinePlayers = listOf(admin, steve)),
            commandExceptionHandler = CommandExceptionHandler(
                multiplatformCommand = multiplatformCommand,
                translationKrate = translationKrate
            )
        )
        val dispatcher = CommandDispatcher<Any>()
        dispatcher.register(builder.create())
        dispatcher.execute(input, Any())
    }

    @Test
    fun GIVEN_linked_player_WHEN_runs_unlink_THEN_link_is_removed_and_reads_success() = runTest {
        linkSteve()

        execute(input = "unlink", sender = steve)

        assertTrue(linkingDao.linkedPlayers.isEmpty())
        assertEquals(listOf<LocalizableComponent>(translation.unlink.success), steve.messages)
    }

    @Test
    fun GIVEN_admin_WHEN_unlinks_a_linked_player_THEN_link_is_removed_and_admin_reads_player_success() = runTest {
        linkSteve()

        execute(input = "unlink Steve", sender = admin)

        assertTrue(linkingDao.linkedPlayers.isEmpty())
        assertEquals(listOf<LocalizableComponent>(translation.unlink.playerSuccess), admin.messages)
    }
}
