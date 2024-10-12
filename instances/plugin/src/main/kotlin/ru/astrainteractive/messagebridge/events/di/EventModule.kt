package ru.astrainteractive.messagebridge.events.di

import kotlinx.coroutines.cancel
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.di.ClientBridgeModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.events.BridgeEvent
import ru.astrainteractive.messagebridge.events.BukkitEvent
import ru.astrainteractive.messagebridge.events.PluginEventConsumer
import ru.astrainteractive.messagebridge.events.SocketEvent
import ru.astrainteractive.messagebridge.messaging.di.MessagingModule

class EventModule(
    coreModule: CoreModule,
    messagingModule: MessagingModule,
    clientBridgeModule: ClientBridgeModule
) {
    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        pluginBridgeApi = clientBridgeModule.pluginBridgeApi
    )
    private val pluginEventConsumer = PluginEventConsumer(
        pluginBridgeApi = clientBridgeModule.pluginBridgeApi,
        telegramMessageController = messagingModule.telegramMessageController,
        minecraftMessageController = messagingModule.minecraftMessageController,
        clientBridgeApi = clientBridgeModule.clientBridgeApi
    )

    private val bridgeEvent = BridgeEvent(
        clientBridgeApi = clientBridgeModule.clientBridgeApi,
        pluginBridgeApi = clientBridgeModule.pluginBridgeApi
    )
    private val socketEvent = SocketEvent(
        clientBridgeApi = clientBridgeModule.clientBridgeApi,
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
            socketEvent.cancel()
        }
    )
}
