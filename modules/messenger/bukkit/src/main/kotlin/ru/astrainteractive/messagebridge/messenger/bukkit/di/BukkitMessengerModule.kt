package ru.astrainteractive.messagebridge.messenger.bukkit.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.core.bukkit.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.link.api.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.bukkit.event.BukkitEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.internal.MinecraftBEventConsumer

class BukkitMessengerModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    linkModule: LinkModule,
    bEventChannel: BEventChannel
) {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val minecraftBEventConsumer = MinecraftBEventConsumer(
        translationKrate = coreModule.translationKrate,
        linkingDao = linkModule.linkingDao,
        dispatchers = coreModule.dispatchers,
        bEventChannel = bEventChannel,
        scope = moduleIoScope,
    )

    private val bukkitEvent = BukkitEvent(
        configFlow = coreModule.config,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        bEventChannel = bEventChannel,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            minecraftBEventConsumer.start()
            bukkitEvent.onEnable(bukkitCoreModule.plugin)
        },
        onDisable = {
            HandlerList.unregisterAll(bukkitCoreModule.plugin)
            bukkitEvent.onDisable()
            moduleIoScope.cancel()
        }
    )
}
