package ru.astrainteractive.messagebridge.onboarding

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.suggestion.Suggestion
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.util.parse
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.onboarding.fake.FakeCommandRegistrarContext
import ru.astrainteractive.messagebridge.onboarding.fake.FakeKotlinDispatchers
import ru.astrainteractive.messagebridge.onboarding.fake.FakeMessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.fake.FakeMultiplatformCommands
import ru.astrainteractive.messagebridge.onboarding.fake.FakeOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.fake.FakePlatformServer
import ru.astrainteractive.messagebridge.onboarding.fake.RecordingConsoleKCommandSender
import ru.astrainteractive.messagebridge.onboarding.fake.RecordingOnlineKPlayer
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.UUID

/**
 * The plugin as a server runs it, over a config.yml in a temporary folder, with bots whose status the test drives.
 * Every coroutine runs on [scheduler], so a test decides how long the commands wait.
 */
internal class OnboardingFixture : AutoCloseable {
    val translation = PluginTranslation()
    private val folder = Files.createTempDirectory("onboarding").toFile()

    val scheduler = TestCoroutineScheduler()
    val registrar = FakeCommandRegistrarContext()

    val coreModule = CoreModule(
        dataFolder = folder,
        dispatchers = FakeKotlinDispatchers(UnconfinedTestDispatcher(scheduler)),
        platformServer = FakePlatformServer(),
        multiplatformCommand = MultiplatformCommand(FakeMultiplatformCommands()),
        commandRegistrarContextFactory = { _ -> registrar }
    )

    val telegram = FakeMessengerOnboarding()
    val discord = FakeMessengerOnboarding()

    val onboardingModule = OnboardingModule(
        coreModule = coreModule,
        plugin = Lifecycle.Lambda(onReload = { coreModule.lifecycle.onReload() }),
        telegramModule = FakeOnboardingModule(telegram),
        discordModule = FakeOnboardingModule(discord)
    )

    private val dispatcher = CommandDispatcher<Any>()

    val console = RecordingConsoleKCommandSender()
    val admin = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001"),
        name = "Admin",
        permissions = setOf(PluginPermission.Setup, PluginPermission.Reload)
    )
    val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve",
        permissions = emptySet()
    )

    /** config.yml of the test server. */
    val configFile: File = folder.resolve("config.yml")

    /** config.yml as it is on disk. */
    val savedConfig: PluginConfiguration
        get() = coreModule.yaml.parse(PluginConfiguration.serializer(), configFile).getOrThrow()

    val consoleReplies: List<String>
        get() = console.messages.map(::plainTextOf)

    init {
        onboardingModule.lifecycle.onEnable()
        registrar.registrations.forEach { registration ->
            @Suppress("UNCHECKED_CAST")
            dispatcher.register(registration.node as LiteralArgumentBuilder<Any>)
        }
    }

    fun plainTextOf(message: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(message.toComponent(Locale.ROOT))
    }

    fun repliesOf(player: RecordingOnlineKPlayer): List<String> = player.messages.map(::plainTextOf)

    /** Saves [change] the way the plugin does, so the bots run with it. */
    fun editConfig(change: (PluginConfiguration) -> PluginConfiguration) {
        coreModule.configKrate.save { loaded -> loaded.map(change) }
    }

    /** Writes config.yml the way an admin does: the bots run with it only after a reload. */
    fun writeConfig(config: PluginConfiguration) {
        configFile.writeText(coreModule.yaml.encodeToString(PluginConfiguration.serializer(), config))
    }

    fun execute(input: String, sender: KCommandSender) {
        dispatcher.execute(input, sender)
    }

    fun execute(input: String) {
        execute(input, console)
    }

    fun execute(input: String, player: RecordingOnlineKPlayer) {
        execute(input, KPlayerKCommandSender(player))
    }

    /** What the console is offered to complete [input] with. */
    fun suggestionsOf(input: String): List<Suggestion> {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, console)).get().list
    }

    override fun close() {
        coreModule.ioScope.cancel()
        coreModule.unconfinedScope.cancel()
        folder.deleteRecursively()
    }
}
