package ru.astrainteractive.messagebridge.commands.setup

import com.charleskorn.kaml.Yaml
import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.commands.fake.FakeMessengerSetup
import ru.astrainteractive.messagebridge.commands.fake.FakeMultiplatformCommands
import ru.astrainteractive.messagebridge.commands.fake.RecordingConsoleKCommandSender
import ru.astrainteractive.messagebridge.commands.fake.RecordingOnlineKPlayer
import ru.astrainteractive.messagebridge.commands.reload.ReloadLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.config.YamlConfigFile
import java.nio.file.Files
import java.util.Locale
import java.util.UUID

/** The whole /mb tree over a config.yml in a temporary folder, with bots whose status the test drives. */
internal class SetupCommandFixture : AutoCloseable {
    val translation = PluginTranslation()
    private val translationKrate = DefaultMutableKrate(factory = { translation }, loader = { null }).asCachedKrate()
    private val folder = Files.createTempDirectory("setup").toFile()
    private val yaml = Yaml(configuration = Yaml.default.configuration.copy(encodeDefaults = true, strictMode = false))

    val configFile = YamlConfigFile(
        stringFormat = yaml,
        serializer = PluginConfiguration.serializer(),
        file = folder.resolve("config.yml"),
        factory = ::PluginConfiguration
    )
    val configKrate = configFile.krate()
    private val translationFile = YamlConfigFile(
        stringFormat = yaml,
        serializer = PluginTranslation.serializer(),
        file = folder.resolve("translations.yml"),
        factory = ::PluginTranslation
    )

    val telegramSetup = FakeMessengerSetup()
    val discordSetup = FakeMessengerSetup()

    val scheduler = TestCoroutineScheduler()
    private val ioScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(scheduler))

    private val plugin = Lifecycle.Lambda(
        onReload = {
            configKrate.getValue()
            translationFile.load()
        }
    )

    val console = RecordingConsoleKCommandSender()
    val admin = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001"),
        name = "Admin",
        permissions = setOf(PluginPermission.Setup, PluginPermission.Reload)
    )
    val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve"
    )

    /** config.yml as it is on disk. */
    val savedConfig: PluginConfiguration
        get() = configFile.parse().getOrThrow()

    fun execute(input: String, sender: KCommandSender = console) {
        val multiplatformCommand = MultiplatformCommand(FakeMultiplatformCommands(sender))
        val commandExceptionHandler = CommandExceptionHandler(
            multiplatformCommand = multiplatformCommand,
            translationKrate = translationKrate
        )
        val executor = SetupCommandExecutor(
            configFile = configFile,
            configKrate = configKrate,
            translationKrate = translationKrate
        )
        val mb = MbLiteralArgumentBuilder(
            subcommands = listOf(
                StatusLiteralArgumentBuilder(
                    telegramSetup = telegramSetup,
                    discordSetup = discordSetup,
                    executor = executor,
                    multiplatformCommand = multiplatformCommand,
                    commandExceptionHandler = commandExceptionHandler,
                    translationKrate = translationKrate
                ).create(),
                ReloadLiteralArgumentBuilder(
                    plugin = plugin,
                    files = listOf(configFile, translationFile),
                    multiplatformCommand = multiplatformCommand,
                    commandExceptionHandler = commandExceptionHandler,
                    translationKrate = translationKrate
                ).create(),
                TelegramLiteralArgumentBuilder(
                    setup = telegramSetup,
                    executor = executor,
                    multiplatformCommand = multiplatformCommand,
                    commandExceptionHandler = commandExceptionHandler,
                    ioScope = ioScope,
                    translationKrate = translationKrate
                ).create(),
                DiscordLiteralArgumentBuilder(
                    setup = discordSetup,
                    executor = executor,
                    multiplatformCommand = multiplatformCommand,
                    commandExceptionHandler = commandExceptionHandler,
                    ioScope = ioScope,
                    translationKrate = translationKrate
                ).create()
            ),
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler,
            translationKrate = translationKrate
        )
        val dispatcher = CommandDispatcher<Any>()
        dispatcher.register(mb.create())
        dispatcher.execute(input, Any())
    }

    fun execute(input: String, player: RecordingOnlineKPlayer) {
        execute(input, KPlayerKCommandSender(player))
    }

    fun plainTextOf(message: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(message.toComponent(Locale.ROOT))
    }

    val consoleReplies: List<String>
        get() = console.messages.map(::plainTextOf)

    fun repliesOf(player: RecordingOnlineKPlayer): List<String> = player.messages.map(::plainTextOf)

    override fun close() {
        ioScope.cancel()
        folder.deleteRecursively()
    }
}
