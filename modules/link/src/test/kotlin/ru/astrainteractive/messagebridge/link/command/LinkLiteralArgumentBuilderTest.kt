@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.command

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.link.code.fake.FakeCodeApi
import ru.astrainteractive.messagebridge.link.fake.FakeMultiplatformCommands
import ru.astrainteractive.messagebridge.link.fake.FakePlatformServer
import ru.astrainteractive.messagebridge.link.fake.RecordingOnlineKPlayer
import ru.astrainteractive.messagebridge.link.player.fake.FakeLinkingDao
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkLiteralArgumentBuilderTest {
    private val translation = PluginTranslation()
    private val translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    private val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve"
    )
    private val multiplatformCommand = MultiplatformCommand(FakeMultiplatformCommands(steve))

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private fun plainTextOf(message: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(message.toComponent(Locale.ROOT))
    }

    private fun executeAsSteve(input: String, codeApi: FakeCodeApi) {
        val builder = LinkLiteralArgumentBuilder(
            executor = LinkCommandExecutor(
                codeApi = codeApi,
                linkingDao = FakeLinkingDao(),
                translationKrate = translationKrate
            ),
            ioScope = ioScope,
            multiplatformCommand = multiplatformCommand,
            platformServer = FakePlatformServer(onlinePlayers = listOf(steve)),
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
    fun GIVEN_player_WHEN_runs_link_THEN_reads_the_created_code() {
        executeAsSteve(input = "link", codeApi = FakeCodeApi(code = 4821))

        assertEquals(listOf(plainTextOf(translation.link.codeCreated(4821))), steve.messages.map(::plainTextOf))
    }

    @Test
    fun GIVEN_code_service_that_breaks_WHEN_player_runs_link_THEN_reads_unknown_error() {
        val brokenCodeApi = FakeCodeApi(code = 4821, failure = IllegalStateException("Code storage is full"))

        executeAsSteve(input = "link", codeApi = brokenCodeApi)

        assertEquals(listOf<LocalizableComponent>(translation.commandError.unknownError), steve.messages)
    }

    @Test
    fun GIVEN_player_without_links_WHEN_someone_asks_for_their_links_THEN_reads_player_not_linked() {
        executeAsSteve(input = "link Steve", codeApi = FakeCodeApi(code = 4821))

        assertEquals(listOf<LocalizableComponent>(translation.unlink.playerNotLinked), steve.messages)
    }
}
