package ru.astrainteractive.messagebridge.link.telegram.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.di.LinkModule
import ru.astrainteractive.messagebridge.link.api.di.LinkTranslationModule
import ru.astrainteractive.messagebridge.link.telegram.command.TelegramLinkHandler
import ru.astrainteractive.messagebridge.link.telegram.event.TelegramLinkInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.di.TelegramBotModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState

class TelegramLinkModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    linkTranslationModule: LinkTranslationModule,
    botModule: TelegramBotModule,
) {
    private val scope = CoroutineScope(
        coreModule.ioScope.coroutineContext + SupervisorJob(coreModule.ioScope.coroutineContext[Job])
    )

    val updateInterceptor: TelegramUpdateInterceptor = TelegramLinkInterceptor(
        scope = scope,
        configFlow = coreModule.config,
        botUserName = { (botModule.state.value as? TelegramConnectionState.Connected)?.botName?.removePrefix("@") },
        linkHandler = TelegramLinkHandler(
            linkApi = linkModule.linkApi,
            messageSender = botModule.messageSender,
            translationKrate = linkTranslationModule.translationKrate,
        ),
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = { scope.cancel() }
    )
}
