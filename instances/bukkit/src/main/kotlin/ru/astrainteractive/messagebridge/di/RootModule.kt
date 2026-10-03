package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.command.PaperMultiplatformCommands
import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.BukkitPlatformServer
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.impl.BEventChannelImpl
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.di.BukkitMessengerModule
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule

class RootModule(
    plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl").withoutParentHandlers() {

    val bukkitCoreModule = BukkitCoreModule(plugin)

    val coreModule = CoreModule(
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

    private val bEventChannel: BEventChannel = BEventChannelImpl()

    val linkModule = LinkModule.Default(coreModule, LuckPermsProvider.Default)

    val bukkitMessengerModule = BukkitMessengerModule(
        coreModule = coreModule,
        bukkitCoreModule = bukkitCoreModule,
        linkingDao = linkModule.linkingDao,
        bEventChannel = bEventChannel
    )

    val jdaMessengerModule = JdaMessengerModule(
        coreModule = coreModule,
        linkModule = linkModule,
        bEventChannel = bEventChannel
    )

    val telegramMessengerModule = TelegramMessengerModule(
        coreModule = coreModule,
        linkModule = linkModule,
        bEventChannel = bEventChannel
    )

    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            linkModule = linkModule,
            lifecyclePlugin = plugin,
            commandRegistrarContext = coreModule.commandRegistrarContext
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            bukkitMessengerModule.lifecycle,
            jdaMessengerModule.lifecycle,
            telegramMessengerModule.lifecycle,
            commandModule.lifecycle
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
