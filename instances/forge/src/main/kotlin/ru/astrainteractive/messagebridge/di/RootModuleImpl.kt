package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.fml.loading.FMLPaths
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.mikro.core.dispatchers.DefaultKotlinDispatchers
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.di.factory.ForgeLuckPermsProvider
import ru.astrainteractive.messagebridge.di.factory.ForgeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.forge.core.event.flowEvent
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.forge.di.ForgeMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule

class RootModuleImpl : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    val coreModule by lazy {
        CoreModule(
            dataFolder = FMLPaths.CONFIGDIR.get().resolve("MessageBridge").toAbsolutePath().toFile(),
            dispatchers = DefaultKotlinDispatchers
        )
    }

    private val serverStateFlow = flowEvent<ServerStartedEvent>()
        .map { event -> event.server }
        .stateIn(coreModule.scope, SharingStarted.Eagerly, null)

    val forgeOnlinePlayersProvider by lazy {
        ForgeOnlinePlayersProvider(serverStateFlow = serverStateFlow)
    }

    val linkModule by lazy {
        LinkModule.Default(coreModule, ForgeLuckPermsProvider)
    }

    val forgeMessengerModule by lazy {
        ForgeMessengerModule(
            coreModule = coreModule,
            serverStateFlow = serverStateFlow
        )
    }

    val jdaEventModule by lazy {
        JdaMessengerModule(
            coreModule = coreModule,
            onlinePlayersProvider = forgeOnlinePlayersProvider,
            linkModule = linkModule
        )
    }

    val tgEventModule by lazy {
        TelegramMessengerModule(
            coreModule = coreModule,
            onlinePlayersProvider = forgeOnlinePlayersProvider,
            linkModule = linkModule
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            // event
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
            forgeMessengerModule.lifecycle
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
