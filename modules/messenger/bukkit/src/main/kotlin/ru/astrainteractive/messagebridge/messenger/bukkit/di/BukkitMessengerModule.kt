package ru.astrainteractive.messagebridge.messenger.bukkit.di

import kotlinx.coroutines.cancel
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.core.bukkit.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.link.player.api.LinkingDao
import ru.astrainteractive.messagebridge.messenger.bukkit.event.BukkitEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.internal.MinecraftBEventConsumer

class BukkitMessengerModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    linkingDao: LinkingDao
) {
    private val minecraftBEventConsumer = MinecraftBEventConsumer(
        translationKrate = coreModule.translationKrate,
        linkingDao = linkingDao,
        dispatchers = coreModule.dispatchers
    )

    private val bukkitEvent = BukkitEvent(
        configFlow = coreModule.config,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
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
