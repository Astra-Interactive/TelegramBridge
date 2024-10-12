package ru.astrainteractive.discordbot.module.bridge.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.WebSocketServer
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.ServerBridgeApi

interface ServerBridgeModule {
    val serverBridgeApi: BridgeApi

    val lifecycle: Lifecycle

    class Default : ServerBridgeModule {
        private val socketServer by lazy {
            WebSocketServer(
                host = "0.0.0.0",
                port = 1111
            )
        }
        override val serverBridgeApi: BridgeApi by lazy {
            ServerBridgeApi(socketServer)
        }

        override val lifecycle: Lifecycle = Lifecycle.Lambda(
            onEnable = {
                socketServer.start()
            },
            onDisable = {
                socketServer.stop(0, "Server closed")
            }
        )
    }
}
