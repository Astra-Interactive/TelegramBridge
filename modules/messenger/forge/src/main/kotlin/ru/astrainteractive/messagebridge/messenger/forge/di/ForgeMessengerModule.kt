package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import net.minecraft.server.MinecraftServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.messenger.forge.event.ForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.messaging.ForgeBEventConsumer

class ForgeMessengerModule(
    coreModule: CoreModule,
    serverStateFlow: StateFlow<MinecraftServer?>
) {

    val eventBukkitMessengerModule = ForgeEvents(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers
    )
    private val minecraftMessageController = ForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
        serverStateFlow = serverStateFlow,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            minecraftMessageController.cancel()
        }
    )
}
