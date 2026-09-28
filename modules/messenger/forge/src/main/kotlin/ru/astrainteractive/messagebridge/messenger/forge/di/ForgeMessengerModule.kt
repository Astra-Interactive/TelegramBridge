package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.forge.event.ForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.messaging.ForgeBEventConsumer

class ForgeMessengerModule(
    coreModule: CoreModule,
) {

    val eventForgeMessengerModule = ForgeEvents(
        configKrate = coreModule.configKrate,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers
    )
    private val minecraftMessageController = ForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = {
            minecraftMessageController.cancel()
        }
    )
}
