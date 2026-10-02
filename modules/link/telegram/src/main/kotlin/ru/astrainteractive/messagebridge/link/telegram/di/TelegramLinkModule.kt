package ru.astrainteractive.messagebridge.link.telegram.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.link.api.di.LinkModule
import ru.astrainteractive.messagebridge.link.api.di.LinkTranslationModule
import ru.astrainteractive.messagebridge.link.telegram.command.TelegramLinkHandler
import ru.astrainteractive.messagebridge.link.telegram.event.TelegramLinkInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramUpdateInterceptor
import ru.astrainteractive.messagebridge.messenger.telegram.api.di.TelegramBotModule
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.botUserName

class TelegramLinkModule(
    coreModule: CoreModule,
    linkModule: LinkModule,
    linkTranslationModule: LinkTranslationModule,
    botModule: TelegramBotModule,
) {
    private val moduleIoScope = coreModule.ioScope.coroutineContext.job
        .let(::SupervisorJob)
        .let(coreModule.ioScope.coroutineContext::plus)
        .let(::CoroutineScope)

    val updateInterceptor: TelegramUpdateInterceptor = TelegramLinkInterceptor(
        scope = moduleIoScope,
        configFlow = coreModule.config,
        botUserName = { botModule.state.value.botUserName },
        linkHandler = TelegramLinkHandler(
            linkApi = linkModule.linkApi,
            messageSender = botModule.messageSender,
            translationKrate = linkTranslationModule.translationKrate,
        ),
    )

    val lifecycle = Lifecycle.Lambda(
        onDisable = { moduleIoScope.cancel() }
    )
}
