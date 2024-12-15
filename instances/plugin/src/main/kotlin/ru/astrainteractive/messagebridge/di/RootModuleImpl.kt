package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.async.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.di.factory.BukkitLuckPermsProvider
import ru.astrainteractive.messagebridge.di.factory.BukkitOnlinePlayersProvider
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.di.BukkitMessengerModule
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule

class RootModuleImpl(
    plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {

    val bukkitCoreModule = BukkitCoreModule(plugin)

    val coreModule = CoreModule(
        dataFolder = bukkitCoreModule.plugin.dataFolder,
        dispatchers = DefaultBukkitDispatchers(bukkitCoreModule.plugin)
    )

    val linkModule = LinkModule.Default(coreModule, BukkitLuckPermsProvider)

    val kyoriKrate = DefaultMutableKrate<KyoriComponentSerializer>(
        factory = { KyoriComponentSerializer.Legacy },
        loader = { null }
    )

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
            bukkitCoreModule = bukkitCoreModule,
            linkModule = linkModule,
            kyoriKrate = kyoriKrate
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            // event
            bukkitMessengerModule.lifecycle,
            jdaMessengerModule.lifecycle,
            telegramMessengerModule.lifecycle,
            // other
            commandModule.lifecycle
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            GlobalScope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            GlobalScope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
            lifecycles.forEach(Lifecycle::onDisable)
        }
    )
}
