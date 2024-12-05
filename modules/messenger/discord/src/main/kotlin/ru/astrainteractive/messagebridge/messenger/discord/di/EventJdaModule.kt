package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.OnlinePlayersProvider
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messenger.discord.event.MessageEventListener

class EventJdaModule(
    coreModule: CoreModule,
    coreJdaModule: CoreJdaModule,
    linkModule: LinkModule,
    telegramMessageController: MessageController,
    minecraftMessageController: MessageController,
    onlinePlayersProvider: OnlinePlayersProvider
) {

    private val messageEventListener = MessageEventListener(
        configKrate = coreModule.configKrate,
        telegramMessageController = telegramMessageController,
        minecraftMessageController = minecraftMessageController,
        onlinePlayersProvider = onlinePlayersProvider,
        translationKrate = coreModule.translationKrate,
        linkApi = linkModule.linkApi
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreJdaModule.jdaFlow
                .onEach { messageEventListener.onEnable(it) }
                .launchIn(coreModule.scope)
        },
        onDisable = {
            messageEventListener.cancel()
            GlobalScope.launch {
                coreJdaModule.jdaFlow.firstOrNull()?.let { jda ->
                    messageEventListener.onDisable(jda)
                    jda.registeredListeners.forEach(jda::removeEventListener)
                    jda.shutdownNow()
                    jda.awaitShutdown()
                }
            }
        }
    )
}
