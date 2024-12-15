package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import net.minecraftforge.fml.loading.FMLPaths
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.klibs.mikro.core.dispatchers.DefaultKotlinDispatchers
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.di.factory.ForgeLuckPermsProvider
import ru.astrainteractive.messagebridge.di.factory.ForgeOnlinePlayersProvider
import ru.astrainteractive.messagebridge.event.ForgeEvents
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.ForgeBEventConsumer
import ru.astrainteractive.messagebridge.messaging.internal.BEventChannel
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule

class RootModuleImpl : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    var minecraftServer: MinecraftServer? = null

    val coreModule by lazy {
        CoreModule(
            dataFolder = FMLPaths.CONFIGDIR.get().resolve("MessageBridge").toAbsolutePath().toFile(),
            dispatchers = DefaultKotlinDispatchers
        )
    }

    val forgeOnlinePlayersProvider by lazy {
        ForgeOnlinePlayersProvider(getServer = { minecraftServer })
    }

    val linkModule by lazy {
        LinkModule.Default(coreModule, ForgeLuckPermsProvider)
    }

    val eventBukkitMessengerModule by lazy {
        ForgeEvents(
            configKrate = coreModule.configKrate,
            scope = coreModule.scope,
            dispatchers = coreModule.dispatchers
        )
    }

    private val minecraftMessageController by lazy {
        ForgeBEventConsumer(
            translationKrate = coreModule.translationKrate,
            getServer = { minecraftServer },
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
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            GlobalScope.launch {
                BEventChannel.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
            eventBukkitMessengerModule.register()
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            minecraftMessageController.cancel()
            GlobalScope.launch {
                BEventChannel.consume(ServerClosedBEvent)
            }
            lifecycles.forEach(Lifecycle::onDisable)
            eventBukkitMessengerModule.unregister()
        }
    )
}
