package ru.astrainteractive.messagebridge.messenger.neoforge.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannel
import ru.astrainteractive.messagebridge.messenger.neoforge.event.NeoForgeEvents
import ru.astrainteractive.messagebridge.messenger.neoforge.internal.NeoForgeBEventConsumer

class NeoForgeMessengerModule(
    coreModule: CoreModule,
    bEventChannel: BEventChannel,
) {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    private val neoForgeEvents = NeoForgeEvents(
        configFlow = coreModule.config,
        ioScope = coreModule.ioScope,
        dispatchers = coreModule.dispatchers,
        bEventChannel = bEventChannel
    )
    private val bEventConsumer = NeoForgeBEventConsumer(
        translationKrate = coreModule.translationKrate,
        bEventChannel = bEventChannel,
        scope = moduleIoScope,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = { bEventConsumer.start() },
        onDisable = { moduleIoScope.cancel() }
    )
}
