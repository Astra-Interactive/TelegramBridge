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
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.impl.BEventChannelImpl
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.forge.di.ForgeMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule
import java.io.File

class RootModule(
    forgeLifecycleServer: ForgeLifecycleServer
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl").withoutParentHandlers() {
    val coreModule = CoreModule(
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

    private val bEventChannel: BEventChannel = BEventChannelImpl()

    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            linkModule = linkModule,
            lifecyclePlugin = forgeLifecycleServer,
            commandRegistrarContext = coreModule.commandRegistrarContext
        )
    }

    val linkModule by lazy {
        LinkModule.Default(coreModule, LuckPermsProvider.Default)
    }

    val forgeMessengerModule by lazy {
        ForgeMessengerModule(
            coreModule = coreModule,
            bEventChannel = bEventChannel,
        )
    }

    val jdaEventModule by lazy {
        JdaMessengerModule(
            coreModule = coreModule,
            linkModule = linkModule,
            bEventChannel = bEventChannel
        )
    }

    val tgEventModule by lazy {
        TelegramMessengerModule(
            coreModule = coreModule,
            linkModule = linkModule,
            bEventChannel = bEventChannel
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            commandModule.lifecycle,
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
