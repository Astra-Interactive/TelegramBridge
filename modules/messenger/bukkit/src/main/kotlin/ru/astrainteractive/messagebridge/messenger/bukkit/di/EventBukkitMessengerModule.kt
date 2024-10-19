package ru.astrainteractive.messagebridge.messenger.bukkit.di

import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messenger.bukkit.events.BukkitEvent

class EventBukkitMessengerModule(
    coreModule: CoreModule,
    telegramMessageController: MessageController,
    discordMessageController: MessageController,
) {
    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        telegramMessageController = telegramMessageController,
        discordMessageController = discordMessageController
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            bukkitEvent.onEnable(coreModule.plugin)
        },
        onDisable = {
            HandlerList.unregisterAll(coreModule.plugin)
            bukkitEvent.onDisable()
        }
    )
}
