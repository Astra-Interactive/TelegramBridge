package ru.astrainteractive.messagebridge.events.di

import kotlinx.coroutines.cancel
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.di.BridgeModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.events.BridgeEvent
import ru.astrainteractive.messagebridge.events.BukkitEvent
import ru.astrainteractive.messagebridge.events.PluginEventConsumer
import ru.astrainteractive.messagebridge.messaging.di.MessagingModule

class EventModule(
    coreModule: CoreModule,
    messagingModule: MessagingModule,
    bridgeModule: BridgeModule
) {
    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        pluginBridgeApi = bridgeModule.pluginBridgeApi
    )
    private val pluginEventConsumer = PluginEventConsumer(
        pluginBridgeApi = bridgeModule.pluginBridgeApi,
        telegramMessageController = messagingModule.telegramMessageController,
        minecraftMessageController = messagingModule.minecraftMessageController,
        clientBridgeApi = bridgeModule.clientBridgeApi
    )

    private val bridgeEvent = BridgeEvent(
        clientBridgeApi = bridgeModule.clientBridgeApi,
        minecraftMessageController = messagingModule.minecraftMessageController,
        telegramMessageController = messagingModule.telegramMessageController
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            bukkitEvent.onEnable(coreModule.plugin)
        },
        onDisable = {
            HandlerList.unregisterAll(coreModule.plugin)
            bukkitEvent.onDisable()
            bridgeEvent.cancel()
            pluginEventConsumer.cancel()
        }
    )
}
