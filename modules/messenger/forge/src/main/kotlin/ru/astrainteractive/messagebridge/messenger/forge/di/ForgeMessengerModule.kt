package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.forge.event.ForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.internal.ForgeBEventConsumer

class ForgeMessengerModule(
    coreModule: CoreModule,
) {

    internal val eventForgeMessengerModule = ForgeEvents(
        configFlow = coreModule.config,
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
