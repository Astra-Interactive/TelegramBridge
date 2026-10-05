package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.forge.event.ForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.internal.ForgeBEventConsumer

class ForgeMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
) {

    val forgeEvents = ForgeEvents(
        configKrate = coreModule.configKrate,
        ioScope = coreModule.ioScope,
        bEventConsumer = bEventChannel
    )
    private val minecraftMessageController = ForgeBEventConsumer(
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
