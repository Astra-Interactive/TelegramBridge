package ru.astrainteractive.messagebridge.messenger.bukkit.di

import kotlinx.coroutines.cancel
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messaging.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.bukkit.events.BukkitEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.messaging.MinecraftBEventConsumer

class BukkitMessengerModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    bEventChannel: BEventChannel,
    textInterceptors: List<TextInterceptor>
) {
    private val minecraftBEventConsumer = MinecraftBEventConsumer(
        translationKrate = coreModule.translationKrate,
        textInterceptors = textInterceptors,
        dispatchers = coreModule.dispatchers,
        bEventReceiver = bEventChannel
    )

    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        bEventConsumer = bEventChannel,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            bukkitEvent.onEnable(bukkitCoreModule.plugin)
        },
        onDisable = {
            HandlerList.unregisterAll(bukkitCoreModule.plugin)
            bukkitEvent.onDisable()
            minecraftBEventConsumer.cancel()
        }
    )
}
