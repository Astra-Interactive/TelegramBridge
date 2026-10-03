package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.forge.event.NeoForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.messaging.NeoForgeBEventConsumer

class NeoForgeMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
) {

    val eventBukkitMessengerModule = NeoForgeEvents(
        configKrate = coreModule.configKrate,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        bEventChannel = bEventChannel
    )
    private val minecraftMessageController = NeoForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
        bEventChannel = bEventChannel,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            minecraftMessageController.cancel()
        }
    )
}
