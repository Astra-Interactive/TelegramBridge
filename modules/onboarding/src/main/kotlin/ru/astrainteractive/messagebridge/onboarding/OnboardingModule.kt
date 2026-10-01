package ru.astrainteractive.messagebridge.onboarding

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.onboarding.bind.BindLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.check.CheckLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.discord.DiscordBindInstruction
import ru.astrainteractive.messagebridge.onboarding.discord.DiscordMessenger
import ru.astrainteractive.messagebridge.onboarding.discord.DiscordProxyTypes
import ru.astrainteractive.messagebridge.onboarding.discord.DiscordSettingLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.discord.DiscordSettings
import ru.astrainteractive.messagebridge.onboarding.discord.DiscordStatusReport
import ru.astrainteractive.messagebridge.onboarding.discord.InviteLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.guide.GuideLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.proxy.AllProxyTypes
import ru.astrainteractive.messagebridge.onboarding.proxy.ProxyLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.proxy.ProxySettings
import ru.astrainteractive.messagebridge.onboarding.reload.ReloadLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.secret.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.setting.SettingCommand
import ru.astrainteractive.messagebridge.onboarding.setting.SettingSaver
import ru.astrainteractive.messagebridge.onboarding.status.StatusLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.status.StatusText
import ru.astrainteractive.messagebridge.onboarding.telegram.TelegramBindInstruction
import ru.astrainteractive.messagebridge.onboarding.telegram.TelegramMessenger
import ru.astrainteractive.messagebridge.onboarding.telegram.TelegramSettingLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.telegram.TelegramSettings
import ru.astrainteractive.messagebridge.onboarding.telegram.TelegramStatusReport
import kotlin.time.Duration.Companion.seconds

/**
 * The /mb commands that set up the Telegram and Discord bots from the game or the console.
 *
 * @param plugin reloaded by `/mb reload`
 */
class OnboardingModule(
    private val coreModule: CoreModule,
    plugin: Lifecycle,
    telegramModule: MessengerOnboardingModule<MessengerOnboarding>,
    discordModule: MessengerOnboardingModule<DiscordOnboarding>
) {
    private val translationKrate = coreModule.translationKrate
    private val multiplatformCommand = coreModule.multiplatformCommand
    private val commandExceptionHandler = coreModule.commandExceptionHandler

    private val telegram = TelegramMessenger(telegramModule.onboarding)
    private val discord = DiscordMessenger(discordModule.onboarding)
    private val statusText = StatusText(translationKrate)
    private val secretGuard = SecretGuard(translationKrate)
    private val proxySettings = ProxySettings(secretGuard, translationKrate)
    private val allProxyTypes = AllProxyTypes(translationKrate)

    private val settingCommand = SettingCommand(
        saver = SettingSaver(
            configKrate = coreModule.configKrate,
            config = coreModule.config,
            statusText = statusText,
            connectionTimeout = CONNECTION_TIMEOUT,
            translationKrate = translationKrate
        ),
        ioScope = coreModule.ioScope,
        multiplatformCommand = multiplatformCommand,
        commandExceptionHandler = commandExceptionHandler
    )

    private val telegramReport = TelegramStatusReport(
        messenger = telegram,
        config = coreModule.config,
        statusText = statusText,
        translationKrate = translationKrate
    )

    private val discordReport = DiscordStatusReport(
        messenger = discord,
        config = coreModule.config,
        statusText = statusText,
        translationKrate = translationKrate
    )

    private val telegramSubcommands = TelegramSettingLiteralArgumentBuilder(
        messenger = telegram,
        settings = TelegramSettings(secretGuard, translationKrate),
        settingCommand = settingCommand,
        multiplatformCommand = multiplatformCommand,
        commandExceptionHandler = commandExceptionHandler
    ).create() + listOf(
        ProxyLiteralArgumentBuilder(
            messenger = telegram,
            types = allProxyTypes,
            settings = proxySettings,
            settingCommand = settingCommand,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler
        ).create(),
        BindLiteralArgumentBuilder(
            messenger = telegram,
            instruction = TelegramBindInstruction(telegram, translationKrate),
            statusText = statusText,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler
        ).create(),
        CheckLiteralArgumentBuilder(
            messenger = telegram,
            ioScope = coreModule.ioScope,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler,
            translationKrate = translationKrate
        ).create()
    )

    private val discordSubcommands = DiscordSettingLiteralArgumentBuilder(
        messenger = discord,
        settings = DiscordSettings(secretGuard, translationKrate),
        settingCommand = settingCommand,
        multiplatformCommand = multiplatformCommand,
        commandExceptionHandler = commandExceptionHandler
    ).create() + listOf(
        ProxyLiteralArgumentBuilder(
            messenger = discord,
            types = DiscordProxyTypes(allProxyTypes, translationKrate),
            settings = proxySettings,
            settingCommand = settingCommand,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler
        ).create(),
        BindLiteralArgumentBuilder(
            messenger = discord,
            instruction = DiscordBindInstruction(translationKrate),
            statusText = statusText,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler
        ).create(),
        InviteLiteralArgumentBuilder(
            messenger = discord,
            statusText = statusText,
            ioScope = coreModule.ioScope,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler,
            translationKrate = translationKrate
        ).create(),
        CheckLiteralArgumentBuilder(
            messenger = discord,
            ioScope = coreModule.ioScope,
            multiplatformCommand = multiplatformCommand,
            commandExceptionHandler = commandExceptionHandler,
            translationKrate = translationKrate
        ).create()
    )

    private val mbCommand = MbLiteralArgumentBuilder(
        subcommands = listOf(
            StatusLiteralArgumentBuilder(
                reports = listOf(telegramReport, discordReport),
                multiplatformCommand = multiplatformCommand,
                commandExceptionHandler = commandExceptionHandler,
                translationKrate = translationKrate
            ).create(),
            ReloadLiteralArgumentBuilder(
                plugin = plugin,
                configKrate = coreModule.configKrate,
                multiplatformCommand = multiplatformCommand,
                commandExceptionHandler = commandExceptionHandler,
                translationKrate = translationKrate
            ).create(),
            GuideLiteralArgumentBuilder(
                messenger = telegram,
                guide = { translationKrate.cachedValue.telegram.guide },
                report = telegramReport,
                subcommands = telegramSubcommands,
                multiplatformCommand = multiplatformCommand,
                commandExceptionHandler = commandExceptionHandler
            ).create(),
            GuideLiteralArgumentBuilder(
                messenger = discord,
                guide = { translationKrate.cachedValue.discord.guide },
                report = discordReport,
                subcommands = discordSubcommands,
                multiplatformCommand = multiplatformCommand,
                commandExceptionHandler = commandExceptionHandler
            ).create()
        ),
        multiplatformCommand = multiplatformCommand,
        commandExceptionHandler = commandExceptionHandler,
        translationKrate = translationKrate
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.commandRegistrarContext.registerWhenReady(
                node = mbCommand.create(),
                scope = coreModule.unconfinedScope
            )
        }
    )

    private companion object {
        val CONNECTION_TIMEOUT = 20.seconds
    }
}
