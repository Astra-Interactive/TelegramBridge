package ru.astrainteractive.messagebridge.messenger.fabric.di

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import net.minecraft.server.MinecraftServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.fabric.event.FabricEvents
import ru.astrainteractive.messagebridge.messenger.fabric.messaging.FabricBEventConsumer

class FabricMessengerModule(
    coreModule: CoreModule,
    serverStateFlow: StateFlow<MinecraftServer?>
) {
    val eventBukkitMessengerModule = FabricEvents(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers
    )
    private val minecraftMessageController = FabricBEventConsumer(
        translationKrate = coreModule.translationKrate,
        serverStateFlow = serverStateFlow,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            minecraftMessageController.cancel()
        }
    )
}
