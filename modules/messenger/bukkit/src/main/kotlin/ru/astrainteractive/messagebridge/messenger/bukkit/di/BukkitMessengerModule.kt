package ru.astrainteractive.messagebridge.messenger.bukkit.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.api.TextInterceptor
import ru.astrainteractive.messagebridge.messenger.bukkit.event.BukkitEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.internal.MinecraftBEventConsumer

class BukkitMessengerModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    bEventChannel: BEventChannel,
    textInterceptors: List<TextInterceptor>
) {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val minecraftBEventConsumer = MinecraftBEventConsumer(
        translationKrate = coreModule.translationKrate,
        textInterceptors = textInterceptors,
        dispatchers = coreModule.dispatchers,
        bEventReceiver = bEventChannel
    )

    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        plugin = bukkitCoreModule.plugin
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            bukkitEvent.bEvents
                .onEach { bEvent -> bEventChannel.consume(bEvent) }
                .launchIn(moduleIoScope)
        },
        onDisable = {
            moduleIoScope.cancel()
            minecraftBEventConsumer.cancel()
        }
    )
}
