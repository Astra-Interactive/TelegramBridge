package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.forge.event.NeoForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.messaging.NeoForgeBEventConsumer

class NeoForgeMessengerModule(
    coreModule: CoreModule,
) {

    val eventBukkitMessengerModule = NeoForgeEvents(
        configKrate = coreModule.configKrate,
        scope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers
    )
    private val minecraftMessageController = NeoForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            minecraftMessageController.cancel()
        }
    )
}
