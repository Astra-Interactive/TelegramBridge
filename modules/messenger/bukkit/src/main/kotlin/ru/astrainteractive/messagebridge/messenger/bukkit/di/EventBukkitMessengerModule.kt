package ru.astrainteractive.messagebridge.messenger.bukkit.di

import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.bukkit.events.BukkitEvent

class EventBukkitMessengerModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    telegramBEventConsumer: BEventConsumer,
    discordBEventConsumer: BEventConsumer,
) {
    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
        telegramBEventConsumer = telegramBEventConsumer,
        discordBEventConsumer = discordBEventConsumer
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            bukkitEvent.onEnable(bukkitCoreModule.plugin)
        },
        onDisable = {
            HandlerList.unregisterAll(bukkitCoreModule.plugin)
            bukkitEvent.onDisable()
        }
    )
}
