package ru.astrainteractive.messagebridge.messenger.neoforge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.neoforge.event.NeoForgeEvents
import ru.astrainteractive.messagebridge.messenger.neoforge.internal.NeoForgeBEventConsumer

class NeoForgeMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
) {

    private val neoForgeEvents = NeoForgeEvents(
        configKrate = coreModule.configKrate,
        ioScope = coreModule.ioScope,
        bEventConsumer = bEventChannel
    )
    private val minecraftMessageController = NeoForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
        dispatchers = coreModule.dispatchers,
        bEventReceiver = bEventChannel,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            minecraftMessageController.cancel()
        }
    )
}
