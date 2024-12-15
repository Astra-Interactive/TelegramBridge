package ru.astrainteractive.messagebridge.di

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
import ru.astrainteractive.messagebridge.messaging.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messaging.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.CoreJdaModule
import ru.astrainteractive.messagebridge.messenger.discord.di.EventJdaModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.CoreTelegramModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramEventModule

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

    val jdaCoreModule by lazy {
        CoreJdaModule(
            coreModule = coreModule,
            linkModule = linkModule,
            onlinePlayersProvider = forgeOnlinePlayersProvider
        )
    }
    val tgCoreModule by lazy {
        CoreTelegramModule(
            coreModule = coreModule
        )
    }

    val eventBukkitMessengerModule by lazy {
        ForgeEvents(
            configKrate = coreModule.configKrate,
            telegramBEventConsumer = tgCoreModule.telegramMessageController,
            discordBEventConsumer = jdaCoreModule.discordMessageController,
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
        EventJdaModule(
            coreModule = coreModule,
            coreJdaModule = jdaCoreModule,
            telegramBEventConsumer = tgCoreModule.telegramMessageController,
            minecraftBEventConsumer = minecraftMessageController,
            onlinePlayersProvider = forgeOnlinePlayersProvider,
            linkModule = linkModule
        )
    }

    val tgEventModule by lazy {
        TelegramEventModule(
            coreModule = coreModule,
            minecraftBEventConsumer = minecraftMessageController,
            discordBEventConsumer = jdaCoreModule.discordMessageController,
            onlinePlayersProvider = forgeOnlinePlayersProvider,
            coreTelegramModule = tgCoreModule,
            linkModule = linkModule
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            // core
            jdaCoreModule.lifecycle,
            tgCoreModule.lifecycle,
            // event
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.scope.launch {
                jdaCoreModule.discordMessageController.consume(ServerOpenBEvent)
                tgCoreModule.telegramMessageController.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
            eventBukkitMessengerModule.register()
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            coreModule.scope.launch {
                jdaCoreModule.discordMessageController.consume(ServerClosedBEvent)
                tgCoreModule.telegramMessageController.consume(ServerClosedBEvent)
            }
            lifecycles.forEach(Lifecycle::onDisable)
            eventBukkitMessengerModule.unregister()
        }
    )
}
