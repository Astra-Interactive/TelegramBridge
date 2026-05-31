package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.launch
import net.neoforged.fml.loading.FMLPaths
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.brigadier.command.MinecraftMultiplatformCommands
import ru.astrainteractive.astralibs.command.registrar.NeoForgeCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.MinecraftDispatchers
import ru.astrainteractive.astralibs.lifecycle.ForgeLifecycleServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.MinecraftPlatformServer
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.forge.core.api.NeoForgeLuckPermsProvider
import ru.astrainteractive.messagebridge.forge.core.api.NeoForgeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.forge.di.NeoForgeMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule
import java.io.File

class RootModule(
    forgeLifecycleServer: ForgeLifecycleServer
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl").withoutParentHandlers() {
    val coreModule by lazy {
        CoreModule(
            dataFolder = FMLPaths.CONFIGDIR.get()
                .resolve("MessageBridge")
                .toAbsolutePath()
                .toFile()
                .also(File::mkdirs),
            dispatchers = MinecraftDispatchers(),
            platformServer = MinecraftPlatformServer,
            commandRegistrarContextFactory = ::NeoForgeCommandRegistrarContext
        )
    }

    val onlinePlayersProvider by lazy {
        NeoForgeOnlinePlayersProvider()
    }

    val linkModule by lazy {
        LinkModule.Default(coreModule, NeoForgeLuckPermsProvider)
    }

    val neoForgeMessengerModule by lazy {
        NeoForgeMessengerModule(
            coreModule = coreModule,
        )
    }
    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            linkModule = linkModule,
            lifecyclePlugin = forgeLifecycleServer,
            commandRegistrarContext = coreModule.commandRegistrarContext,
            multiplatformCommand = MultiplatformCommand(MinecraftMultiplatformCommands())
        )
    }

    val jdaEventModule by lazy {
        JdaMessengerModule(
            coreModule = coreModule,
            onlinePlayersProvider = onlinePlayersProvider,
            linkModule = linkModule
        )
    }

    val tgEventModule by lazy {
        TelegramMessengerModule(
            coreModule = coreModule,
            onlinePlayersProvider = onlinePlayersProvider,
            linkModule = linkModule
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            commandModule.lifecycle,
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
            neoForgeMessengerModule.lifecycle
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.ioScope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            coreModule.ioScope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
            lifecycles.reversed().forEach(Lifecycle::onDisable)
        }
    )
}
