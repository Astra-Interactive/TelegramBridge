package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.command.PaperMultiplatformCommands
import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.BukkitPlatformServer
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.command.di.CommandModule
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.core.bukkit.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.bukkit.impl.BukkitLuckPermsProvider
import ru.astrainteractive.messagebridge.core.bukkit.impl.BukkitOnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.di.BukkitMessengerModule
import ru.astrainteractive.messagebridge.messenger.discord.impl.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.impl.di.TelegramMessengerModule
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.discord.di.DiscordOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.impl.di.OnboardingModule
import ru.astrainteractive.messagebridge.onboarding.telegram.di.TelegramOnboardingModule

internal class RootModule(
    plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {

    private val bukkitCoreModule = BukkitCoreModule(plugin)

    private val coreModule = CoreModule(
        dataFolder = bukkitCoreModule.plugin.dataFolder,
        dispatchers = DefaultBukkitDispatchers(bukkitCoreModule.plugin),
        platformServer = BukkitPlatformServer(),
        multiplatformCommand = MultiplatformCommand(PaperMultiplatformCommands()),
        commandRegistrarContextFactory = { mainScope ->
            PaperCommandRegistrarContext(
                mainScope = mainScope,
                plugin = plugin
            )
        }
    )

    private val bEventChannel = BEventChannel()

    private val linkModule = LinkModule(coreModule, BukkitLuckPermsProvider)

    private val bukkitMessengerModule = BukkitMessengerModule(
        coreModule = coreModule,
        bukkitCoreModule = bukkitCoreModule,
        linkModule = linkModule,
        bEventChannel = bEventChannel
    )

    private val jdaMessengerModule = JdaMessengerModule(
        coreModule = coreModule,
        onlinePlayersProvider = BukkitOnlinePlayersProvider,
        linkModule = linkModule,
        messageInterceptors = { listOf(discordOnboardingModule.messageInterceptor) },
        bEventChannel = bEventChannel
    )

    private val onboardingTranslationModule by lazy {
        OnboardingTranslationModule(coreModule = coreModule)
    }

    private val discordOnboardingModule: DiscordOnboardingModule by lazy {
        DiscordOnboardingModule(
            coreModule = coreModule,
            onboardingTranslationModule = onboardingTranslationModule,
            botModule = jdaMessengerModule
        )
    }

    private val telegramMessengerModule = TelegramMessengerModule(
        coreModule = coreModule,
        onlinePlayersProvider = BukkitOnlinePlayersProvider,
        linkModule = linkModule,
        updateInterceptors = { listOf(telegramOnboardingModule.updateInterceptor) },
        bEventChannel = bEventChannel
    )

    private val telegramOnboardingModule: TelegramOnboardingModule by lazy {
        TelegramOnboardingModule(
            coreModule = coreModule,
            onboardingTranslationModule = onboardingTranslationModule,
            botModule = telegramMessengerModule
        )
    }

    private val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            linkModule = linkModule
        )
    }

    private val onboardingModule by lazy {
        OnboardingModule(
            coreModule = coreModule,
            plugin = plugin,
            onboardingTranslationModule = onboardingTranslationModule,
            telegramModule = telegramOnboardingModule,
            discordModule = discordOnboardingModule
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            bukkitMessengerModule.lifecycle,
            jdaMessengerModule.lifecycle,
            telegramMessengerModule.lifecycle,
            commandModule.lifecycle,
            onboardingTranslationModule.lifecycle,
            telegramOnboardingModule.lifecycle,
            discordOnboardingModule.lifecycle,
            onboardingModule.lifecycle
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.ioScope.launch {
                bEventChannel.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            coreModule.ioScope.launch {
                bEventChannel.consume(ServerClosedBEvent)
            }
            lifecycles.reversed().forEach(Lifecycle::onDisable)
        }
    )
}
