package ru.astrainteractive.discordbot.module.bridge.di

import kotlinx.coroutines.runBlocking
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.ClientBridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.PluginBridgeApi

interface BridgeModule {
    val clientBridgeApi: BridgeApi
    val pluginBridgeApi: PluginBridgeApi

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

        override val pluginBridgeApi: PluginBridgeApi = PluginBridgeApi()

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
