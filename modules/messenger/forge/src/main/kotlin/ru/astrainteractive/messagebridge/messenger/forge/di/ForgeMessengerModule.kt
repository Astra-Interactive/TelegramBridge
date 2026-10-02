package ru.astrainteractive.messagebridge.messenger.forge.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.forge.event.ForgeEvents
import ru.astrainteractive.messagebridge.messenger.forge.internal.ForgeBEventConsumer

class ForgeMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
) {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val forgeEvents = ForgeEvents(
        configFlow = coreModule.config,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        bEventChannel = bEventChannel
    )
    private val bEventConsumer = ForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
        bEventChannel = bEventChannel,
        scope = moduleIoScope,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = { bEventConsumer.start() },
        onDisable = { moduleIoScope.cancel() }
    )
}
