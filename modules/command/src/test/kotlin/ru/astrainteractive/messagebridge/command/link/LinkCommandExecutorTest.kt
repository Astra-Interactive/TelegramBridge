@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.command.link

import kotlinx.coroutines.test.runTest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.RecordingOnlineKPlayer
import ru.astrainteractive.messagebridge.link.code.fake.FakeCodeApi
import ru.astrainteractive.messagebridge.link.code.model.CodeUser
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import ru.astrainteractive.messagebridge.link.player.model.LinkedPlayerModel
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkCommandExecutorTest {
    private val translation = PluginTranslation()
    private val codeApi = FakeCodeApi(code = 4821)
    private val linkingDao = FakeLinkingDao()
    private val executor = LinkCommandExecutor(
        codeApi = codeApi,
        linkingDao = linkingDao,
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

    private fun plainTextOf(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }

    private fun plainTextOf(message: LocalizableComponent): String {
        return plainTextOf(message.toComponent(Locale.ROOT))
    }

    private fun assertReadOnly(player: RecordingOnlineKPlayer, message: LocalizableComponent) {
        assertEquals(listOf(message), player.messages)
    }

    @Test
    fun GIVEN_player_WHEN_links_THEN_code_is_bound_to_them_and_they_read_it() = runTest {
        executor.onIntent(LinkCommandExecutor.Intent.Link(steve))

        assertEquals(listOf(CodeUser(name = "Steve", uuid = steve.uuid)), codeApi.codeUsers)
        assertEquals(listOf(plainTextOf(translation.link.codeCreated(4821))), steve.messages.map(::plainTextOf))
    }

    @Test
    fun GIVEN_linked_player_WHEN_admin_asks_for_their_links_THEN_admin_reads_them() = runTest {
        linkingDao.upsert(
            LinkedPlayerModel(
                uuid = steve.uuid,
                lastMinecraftName = "Steve",
                discordLink = LinkedPlayerModel.DiscordLink(lastDiscordName = "steve", discordId = 42),
                telegramLink = LinkedPlayerModel.TelegramLink(telegramUsername = "steve_tg", telegramId = 7)
            )
        )

        executor.onIntent(LinkCommandExecutor.Intent.UserInfo(targetPlayerUuid = steve.uuid, sender = admin))

        assertEquals(
            listOf("DiscordID: 42; telegramUsername: steve_tg; minecraftUUID: ${steve.uuid}"),
            admin.components.map(::plainTextOf)
        )
    }

    @Test
    fun GIVEN_player_without_links_WHEN_admin_asks_for_their_links_THEN_admin_reads_player_not_linked() = runTest {
        executor.onIntent(LinkCommandExecutor.Intent.UserInfo(targetPlayerUuid = steve.uuid, sender = admin))

        assertReadOnly(admin, translation.unlink.playerNotLinked)
    }

    @Test
    fun GIVEN_unreadable_database_WHEN_admin_asks_for_links_THEN_admin_reads_unknown_error_not_player_not_linked() =
        runTest {
            linkingDao.failure = IllegalStateException("Database is locked")

            executor.onIntent(LinkCommandExecutor.Intent.UserInfo(targetPlayerUuid = steve.uuid, sender = admin))

            assertReadOnly(admin, translation.commandError.unknownError)
        }
}
