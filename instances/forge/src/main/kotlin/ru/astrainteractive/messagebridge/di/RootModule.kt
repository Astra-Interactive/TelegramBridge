package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.launch
import net.minecraftforge.fml.loading.FMLPaths
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.brigadier.command.MinecraftMultiplatformCommands
import ru.astrainteractive.astralibs.command.registrar.ForgeCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.MinecraftDispatchers
import ru.astrainteractive.astralibs.lifecycle.ForgeLifecycleServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.MinecraftPlatformServer
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.core.forge.impl.ForgeLuckPermsProvider
import ru.astrainteractive.messagebridge.core.forge.impl.ForgeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.api.di.LinkTranslationModule
import ru.astrainteractive.messagebridge.link.discord.di.DiscordLinkModule
import ru.astrainteractive.messagebridge.link.impl.di.LinkModuleImpl
import ru.astrainteractive.messagebridge.link.telegram.di.TelegramLinkModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.impl.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.forge.di.ForgeMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.impl.di.TelegramMessengerModule
import ru.astrainteractive.messagebridge.onboarding.api.di.OnboardingTranslationModule
import ru.astrainteractive.messagebridge.onboarding.discord.di.DiscordOnboardingModule
import ru.astrainteractive.messagebridge.onboarding.impl.di.OnboardingModule
import ru.astrainteractive.messagebridge.onboarding.telegram.di.TelegramOnboardingModule
import java.io.File

internal class RootModule(
    forgeLifecycleServer: ForgeLifecycleServer
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    private val coreModule = CoreModule(
        dataFolder = FMLPaths.CONFIGDIR.get()
            .resolve("MessageBridge")
            .toAbsolutePath()
            .toFile()
            .also(File::mkdirs),
        dispatchers = MinecraftDispatchers(),
        platformServer = MinecraftPlatformServer,
        multiplatformCommand = MultiplatformCommand(MinecraftMultiplatformCommands()),
        commandRegistrarContextFactory = ::ForgeCommandRegistrarContext
    )

    private val onlinePlayersProvider by lazy {
        ForgeOnlinePlayersProvider()
    }

    private val bEventChannel = BEventChannel()

    private val linkTranslationModule by lazy {
        LinkTranslationModule(coreModule = coreModule)
    }

    private val linkModule by lazy {
        LinkModuleImpl(coreModule, ForgeLuckPermsProvider, linkTranslationModule)
    }

    private val forgeMessengerModule by lazy {
        ForgeMessengerModule(
            coreModule = coreModule,
            bEventChannel = bEventChannel,
        )
    }

    private val jdaEventModule by lazy {
        JdaMessengerModule(
            coreModule = coreModule,
            onlinePlayersProvider = onlinePlayersProvider,
            linkModule = linkModule,
            messageInterceptors = {
                listOf(discordOnboardingModule.messageInterceptor, discordLinkModule.messageInterceptor)
            },
            bEventChannel = bEventChannel
        )
    }

    private val onboardingTranslationModule by lazy {
        OnboardingTranslationModule(coreModule = coreModule)
    }

    private val discordOnboardingModule: DiscordOnboardingModule by lazy {
        DiscordOnboardingModule(
            coreModule = coreModule,
            onboardingTranslationModule = onboardingTranslationModule,
            botModule = jdaEventModule
        )
    }

    private val tgEventModule by lazy {
        TelegramMessengerModule(
            coreModule = coreModule,
            onlinePlayersProvider = onlinePlayersProvider,
            updateInterceptors = {
                listOf(telegramOnboardingModule.updateInterceptor, telegramLinkModule.updateInterceptor)
            },
            bEventChannel = bEventChannel
        )
    }

    private val telegramOnboardingModule: TelegramOnboardingModule by lazy {
        TelegramOnboardingModule(
            coreModule = coreModule,
            onboardingTranslationModule = onboardingTranslationModule,
            botModule = tgEventModule
        )
    }

    private val telegramLinkModule: TelegramLinkModule by lazy {
        TelegramLinkModule(
            coreModule = coreModule,
            linkModule = linkModule,
            linkTranslationModule = linkTranslationModule,
            botModule = tgEventModule
        )
    }

    private val discordLinkModule: DiscordLinkModule by lazy {
        DiscordLinkModule(
            coreModule = coreModule,
            linkModule = linkModule,
            linkTranslationModule = linkTranslationModule,
            botModule = jdaEventModule
        )
    }

    private val onboardingModule by lazy {
        OnboardingModule(
            coreModule = coreModule,
            plugin = forgeLifecycleServer,
            onboardingTranslationModule = onboardingTranslationModule,
            telegramModule = telegramOnboardingModule,
            discordModule = discordOnboardingModule
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            linkModule.lifecycle,
            linkTranslationModule.lifecycle,
            telegramLinkModule.lifecycle,
            discordLinkModule.lifecycle,
            onboardingModule.lifecycle,
            onboardingTranslationModule.lifecycle,
            telegramOnboardingModule.lifecycle,
            discordOnboardingModule.lifecycle,
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
            forgeMessengerModule.lifecycle
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
