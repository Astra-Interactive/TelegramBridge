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
import ru.astrainteractive.messagebridge.messaging.ForgeMessageController
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
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
            telegramMessageController = tgCoreModule.telegramMessageController,
            discordMessageController = jdaCoreModule.discordMessageController,
            scope = coreModule.scope,
            dispatchers = coreModule.dispatchers
        )
    }

    private val minecraftMessageController by lazy {
        ForgeMessageController(
            translationKrate = coreModule.translationKrate,
            getServer = { minecraftServer },
        )
    }

    val jdaEventModule by lazy {
        EventJdaModule(
            coreModule = coreModule,
            coreJdaModule = jdaCoreModule,
            telegramMessageController = tgCoreModule.telegramMessageController,
            minecraftMessageController = minecraftMessageController,
            onlinePlayersProvider = forgeOnlinePlayersProvider,
            linkModule = linkModule
        )
    }

    val tgEventModule by lazy {
        TelegramEventModule(
            coreModule = coreModule,
            minecraftMessageController = minecraftMessageController,
            discordMessageController = jdaCoreModule.discordMessageController,
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
                jdaCoreModule.discordMessageController.send(ServerEvent.ServerOpen)
                tgCoreModule.telegramMessageController.send(ServerEvent.ServerOpen)
            }
            lifecycles.forEach(Lifecycle::onEnable)
            eventBukkitMessengerModule.register()
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            coreModule.scope.launch {
                jdaCoreModule.discordMessageController.send(ServerEvent.ServerClosed)
                tgCoreModule.telegramMessageController.send(ServerEvent.ServerClosed)
            }
            lifecycles.forEach(Lifecycle::onDisable)
            eventBukkitMessengerModule.unregister()
        }
    )
}
