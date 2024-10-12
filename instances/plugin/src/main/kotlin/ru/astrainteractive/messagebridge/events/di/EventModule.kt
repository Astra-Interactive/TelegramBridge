package ru.astrainteractive.messagebridge.events.di

import kotlinx.coroutines.cancel
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.di.ClientBridgeModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.di.TelegramModule
import ru.astrainteractive.messagebridge.events.BridgeEvent
import ru.astrainteractive.messagebridge.events.BukkitEvent
import ru.astrainteractive.messagebridge.events.SocketEvent

class EventModule(
    coreModule: CoreModule,
    telegramModule: TelegramModule,
    clientBridgeModule: ClientBridgeModule
) {
    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        clientBridgeApi = clientBridgeModule.clientBridgeApi,
        telegramMessageController = telegramModule.telegramMessageController
    )

    private val bridgeEvent = BridgeEvent(
        clientBridgeApi = clientBridgeModule.clientBridgeApi,
        minecraftMessageController = telegramModule.minecraftMessageController,
        telegramMessageController = telegramModule.telegramMessageController
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
            socketEvent.cancel()
        }
    )
}
