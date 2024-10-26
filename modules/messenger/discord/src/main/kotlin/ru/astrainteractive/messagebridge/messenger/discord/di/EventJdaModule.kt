package ru.astrainteractive.messagebridge.messenger.discord.di

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.MinecraftBridge
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
    minecraftBridge: MinecraftBridge
) {

    private val messageEventListener = MessageEventListener(
        configKrate = coreModule.configKrate,
        telegramMessageController = telegramMessageController,
        minecraftMessageController = minecraftMessageController,
        minecraftBridge = minecraftBridge,
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
            runBlocking {
                coreJdaModule.jdaFlow.first().let { jda ->
                    messageEventListener.onDisable(jda)
                    jda.registeredListeners.forEach(jda::removeEventListener)
                    jda.shutdownNow()
                    jda.awaitShutdown()
                }
            }
        }
    )
}
