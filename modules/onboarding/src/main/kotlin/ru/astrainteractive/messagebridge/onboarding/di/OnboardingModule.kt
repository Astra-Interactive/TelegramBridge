package ru.astrainteractive.messagebridge.onboarding.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.command.BindLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.command.CheckLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.command.GuideLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.command.MbLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.command.ReloadLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.discord.command.DiscordSettingLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.discord.command.InviteLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordBindInstruction
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordMessenger
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordProxyTypes
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordSettings
import ru.astrainteractive.messagebridge.onboarding.discord.internal.DiscordStatusReport
import ru.astrainteractive.messagebridge.onboarding.proxy.command.ProxyLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.proxy.internal.AllProxyTypes
import ru.astrainteractive.messagebridge.onboarding.proxy.internal.ProxySettings
import ru.astrainteractive.messagebridge.onboarding.secret.internal.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.setting.command.SettingCommand
import ru.astrainteractive.messagebridge.onboarding.setting.internal.SettingSaver
import ru.astrainteractive.messagebridge.onboarding.status.command.StatusLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.status.internal.StatusText
import ru.astrainteractive.messagebridge.onboarding.telegram.command.TelegramSettingLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramBindInstruction
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramMessenger
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramSettings
import ru.astrainteractive.messagebridge.onboarding.telegram.internal.TelegramStatusReport
import kotlin.time.Duration.Companion.seconds

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
