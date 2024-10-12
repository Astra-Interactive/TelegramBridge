package ru.astrainteractive.messagebridge.discord.event.di

import kotlinx.coroutines.cancel
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.di.ServerBridgeModule
import ru.astrainteractive.messagebridge.discord.di.JdaModule
import ru.astrainteractive.messagebridge.discord.event.MessageEventListener
import ru.astrainteractive.messagebridge.discord.event.SocketEventListener

class JdaEventModule(
    serverBridgeModule: ServerBridgeModule,
    jdaModule: JdaModule
) {

    val messageEventListener = MessageEventListener(
        serverBridgeApi = serverBridgeModule.serverBridgeApi
    )

    val socketEventListener = SocketEventListener(
        jda = jdaModule.jda,
        serverBridgeApi = serverBridgeModule.serverBridgeApi
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            messageEventListener.onEnable(jdaModule.jda)
        },
        onDisable = {
            messageEventListener.onDisable(jdaModule.jda)
            messageEventListener.cancel()
            socketEventListener.cancel()
        }
    )
}
