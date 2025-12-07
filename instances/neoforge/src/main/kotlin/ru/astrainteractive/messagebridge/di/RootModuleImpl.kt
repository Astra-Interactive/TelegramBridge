package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.neoforged.fml.loading.FMLPaths
import ru.astrainteractive.astralibs.coroutines.NeoForgeDispatchers
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
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

class RootModuleImpl : Logger by JUtiltLogger("MessageBridge-RootModuleImpl").withoutParentHandlers() {
    val coreModule by lazy {
        CoreModule(
            dataFolder = FMLPaths.CONFIGDIR.get()
                .resolve("SoulKeeper")
                .toAbsolutePath()
                .toFile()
                .also(File::mkdirs),
            dispatchers = NeoForgeDispatchers()
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
            // event
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
            neoForgeMessengerModule.lifecycle
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
