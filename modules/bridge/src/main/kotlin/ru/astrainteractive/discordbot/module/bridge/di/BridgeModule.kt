package ru.astrainteractive.discordbot.module.bridge.di

import kotlinx.coroutines.runBlocking
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.ClientBridgeApi

interface BridgeModule {
    val clientBridgeApi: BridgeApi

    val lifecycle: Lifecycle

    class Default : BridgeModule {
        private val socketClient by lazy {
            WebSocketClient(
                host = "89.150.35.252",
                port = 1111
            )
        }
        override val clientBridgeApi: BridgeApi by lazy {
            ClientBridgeApi(socketClient)
        }

        override val lifecycle: Lifecycle = Lifecycle.Lambda(
            onEnable = {
                socketClient.tryOpenConnection()
            },
            onDisable = {
                runBlocking { socketClient.close() }
            }
        )
    }
}
