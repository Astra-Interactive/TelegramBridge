package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fabricmc.loader.api.FabricLoader
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.kstorage.util.asCachedKrate
import ru.astrainteractive.klibs.mikro.core.dispatchers.DefaultKotlinDispatchers
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.api.FabricLuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.FabricOnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.api.FabricServer
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.fabric.di.FabricMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule
import java.io.File

class RootModule : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    val coreModule = CoreModule(
        dataFolder = FabricLoader.getInstance().configDir
            .toFile()
            .resolve("MessageBridge")
            .also(File::mkdirs),
        dispatchers = object : KotlinDispatchers {
            override val Main = DefaultKotlinDispatchers.IO
            override val IO = DefaultKotlinDispatchers.IO
            override val Default = DefaultKotlinDispatchers.Default
            override val Unconfined = DefaultKotlinDispatchers.Unconfined
        }
    )

    val fabricLuckPermsProvider = FabricLuckPermsProvider
    val fabricOnlinePlayersProvider = FabricOnlinePlayersProvider()

    val linkModule = LinkModule.Default(coreModule, fabricLuckPermsProvider)

    val kyoriKrate = DefaultMutableKrate<KyoriComponentSerializer>(
        factory = { KyoriComponentSerializer.Legacy },
        loader = { null }
    ).asCachedKrate()

    val fabricMessengerModule = FabricMessengerModule(
        coreModule = coreModule,
        serverStateFlow = FabricServer.serverFlow
    )

    val jdaMessengerModule = JdaMessengerModule(
        coreModule = coreModule,
        onlinePlayersProvider = fabricOnlinePlayersProvider,
        linkModule = linkModule
    )

    val telegramMessengerModule = TelegramMessengerModule(
        coreModule = coreModule,
        onlinePlayersProvider = fabricOnlinePlayersProvider,
        linkModule = linkModule
    )

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            // event
            fabricMessengerModule.lifecycle,
            jdaMessengerModule.lifecycle,
            telegramMessengerModule.lifecycle,
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
