package ru.astrainteractive.messagebridge.onboarding.impl.fake

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
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.core.api.fake.FakeMultiplatformCommands
import ru.astrainteractive.messagebridge.core.api.fake.FakePlatformServer
import ru.astrainteractive.messagebridge.core.api.fake.RecordingConsoleKCommandSender
import ru.astrainteractive.messagebridge.core.api.fake.RecordingOnlineKPlayer
import ru.astrainteractive.messagebridge.core.api.permission.PluginPermission
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.api.fake.FakeMessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.fake.FakeOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.di.OnboardingModule
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.UUID

internal class OnboardingFixture : AutoCloseable {
    val translation = PluginTranslation()
    val onboardingTranslation = OnboardingTranslation()
    private val folder = Files.createTempDirectory("onboarding").toFile()

    val scheduler = TestCoroutineScheduler()
    val registrar = FakeCommandRegistrarContext()

    val coreModule = CoreModule(
        dataFolder = folder,
        dispatchers = FakeKotlinDispatchers(UnconfinedTestDispatcher(scheduler)),
        platformServer = FakePlatformServer(onlinePlayers = emptyList()),
        multiplatformCommand = MultiplatformCommand(FakeMultiplatformCommands(sender = null)),
        commandRegistrarContextFactory = { _ -> registrar }
    )

    val onboardingTranslationModule = OnboardingTranslationModule(coreModule = coreModule)

    val telegram = FakeMessengerOnboarding()
    val discord = FakeMessengerOnboarding()

    val onboardingModule = OnboardingModule(
        coreModule = coreModule,
        plugin = Lifecycle.Lambda(
            onReload = {
                coreModule.lifecycle.onReload()
                onboardingTranslationModule.lifecycle.onReload()
            }
        ),
        onboardingTranslationModule = onboardingTranslationModule,
        telegramModule = FakeOnboardingModule(telegram),
        discordModule = FakeOnboardingModule(discord)
    )

    private val dispatcher = CommandDispatcher<Any>()

    val console = RecordingConsoleKCommandSender(hasAllPermissions = true)
    val admin = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000001"),
        name = "Admin",
        permissions = setOf(OnboardingPermission.Setup, PluginPermission.Reload)
    )
    val steve = RecordingOnlineKPlayer(
        uuid = UUID.fromString("5e4a7f7a-0000-4000-8000-000000000002"),
        name = "Steve",
        permissions = emptySet()
    )

    val configFile: File = folder.resolve("config.yml")

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

    fun editConfig(change: (PluginConfiguration) -> PluginConfiguration) {
        coreModule.configKrate.save { loaded -> loaded.map(change) }
    }

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

    fun suggestionsOf(input: String): List<Suggestion> {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, console)).get().list
    }

    override fun close() {
        coreModule.ioScope.cancel()
        coreModule.unconfinedScope.cancel()
        folder.deleteRecursively()
    }
}
