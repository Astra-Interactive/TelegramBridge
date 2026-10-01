package ru.astrainteractive.messagebridge.messenger.neoforge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.neoforge.event.NeoForgeEvents
import ru.astrainteractive.messagebridge.messenger.neoforge.internal.NeoForgeBEventConsumer

class NeoForgeMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
) {

    private val eventBukkitMessengerModule = NeoForgeEvents(
        configFlow = coreModule.config,
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
