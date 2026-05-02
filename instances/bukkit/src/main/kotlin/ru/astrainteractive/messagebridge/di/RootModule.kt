package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.command.PaperMultiplatformCommands
import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.BukkitPlatformServer
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.api.BukkitLuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.BukkitOnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
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
        platformServer = BukkitPlatformServer()
    )

    val linkModule = LinkModule.Default(coreModule, BukkitLuckPermsProvider)

    val kyoriKrate = DefaultMutableKrate<KyoriComponentSerializer>(
        factory = { KyoriComponentSerializer.Legacy },
        loader = { null }
    ).asCachedKrate()

    val bukkitMessengerModule = BukkitMessengerModule(
        coreModule = coreModule,
        bukkitCoreModule = bukkitCoreModule,
        kyoriKrate = kyoriKrate,
        linkingDao = linkModule.linkingDao
    )

    val jdaMessengerModule = JdaMessengerModule(
        coreModule = coreModule,
        onlinePlayersProvider = BukkitOnlinePlayersProvider,
        linkModule = linkModule
    )

    val telegramMessengerModule = TelegramMessengerModule(
        coreModule = coreModule,
        onlinePlayersProvider = BukkitOnlinePlayersProvider,
        linkModule = linkModule
    )

    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            linkModule = linkModule,
            kyoriKrate = kyoriKrate,
            lifecyclePlugin = plugin,
            commandRegistrarContext = PaperCommandRegistrarContext(
                mainScope = coreModule.mainScope,
                plugin = plugin
            ),
            multiplatformCommand = MultiplatformCommand(PaperMultiplatformCommands())
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
            GlobalScope.launch(Dispatchers.IO) {
                BEventChannel.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            GlobalScope.launch(Dispatchers.IO) {
                BEventChannel.consume(ServerClosedBEvent)
            }
            lifecycles.forEach(Lifecycle::onDisable)
        }
    )
}
