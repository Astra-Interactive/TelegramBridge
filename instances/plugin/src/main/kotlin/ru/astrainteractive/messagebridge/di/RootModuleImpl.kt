package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.astrainteractive.astralibs.async.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.BukkitLuckPermsFactory
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.MinecraftBridge
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.di.CoreBukkitMessengerModule
import ru.astrainteractive.messagebridge.messenger.bukkit.di.EventBukkitMessengerModule
import ru.astrainteractive.messagebridge.messenger.discord.di.CoreJdaModule
import ru.astrainteractive.messagebridge.messenger.discord.di.EventJdaModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.CoreTelegramModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramEventModule

class RootModuleImpl(
    plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    private val minecraftBridge = object : MinecraftBridge {
        override fun getOnlinePlayers(): List<String> {
            return Bukkit.getOnlinePlayers().map(Player::getDisplayName)
        }
    }

    val bukkitCoreModule = BukkitCoreModule(plugin)

    val coreModule = CoreModule(
        dataFolder = bukkitCoreModule.plugin.dataFolder,
        dispatchers = DefaultBukkitDispatchers(bukkitCoreModule.plugin)
    )

    val linkModule = LinkModule.Default(coreModule, BukkitLuckPermsFactory)

    val coreBukkitMessengerModule = CoreBukkitMessengerModule(
        coreModule = coreModule
    )
    val jdaCoreModule = CoreJdaModule(
        coreModule = coreModule,
    )
    val tgCoreModule = CoreTelegramModule(
        coreModule = coreModule
    )

    val eventBukkitMessengerModule = EventBukkitMessengerModule(
        coreModule = coreModule,
        bukkitCoreModule = bukkitCoreModule,
        telegramMessageController = tgCoreModule.telegramMessageController,
        discordMessageController = jdaCoreModule.discordMessageController
    )

    val jdaEventModule = EventJdaModule(
        coreModule = coreModule,
        coreJdaModule = jdaCoreModule,
        telegramMessageController = tgCoreModule.telegramMessageController,
        minecraftMessageController = coreBukkitMessengerModule.minecraftMessageController,
        minecraftBridge = minecraftBridge,
        linkModule = linkModule
    )

    val tgEventModule = TelegramEventModule(
        coreModule = coreModule,
        minecraftMessageController = coreBukkitMessengerModule.minecraftMessageController,
        discordMessageController = jdaCoreModule.discordMessageController,
        minecraftBridge = minecraftBridge,
        coreTelegramModule = tgCoreModule,
        linkModule = linkModule
    )

    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            bukkitCoreModule = bukkitCoreModule,
            linkModule = linkModule
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            // core
            coreBukkitMessengerModule.lifecycle,
            jdaCoreModule.lifecycle,
            tgCoreModule.lifecycle,
            // event
            eventBukkitMessengerModule.lifecycle,
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
            // other
            commandModule.lifecycle
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.scope.launch {
                jdaCoreModule.discordMessageController.send(ServerEvent.ServerOpen)
                tgCoreModule.telegramMessageController.send(ServerEvent.ServerOpen)
            }
            lifecycles.forEach(Lifecycle::onEnable)
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
        }
    )
}
