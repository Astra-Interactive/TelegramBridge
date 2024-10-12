package ru.astrainteractive.discordbot.module.bridge.di

import kotlinx.coroutines.runBlocking
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.ClientBridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.PluginBridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

interface ClientBridgeModule {
    val clientBridgeApi: BridgeApi
    val pluginBridgeApi: PluginBridgeApi

    val lifecycle: Lifecycle

    class Default : ClientBridgeModule {
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
                runBlocking {
                    pluginBridgeApi.broadcastEvent(ServerEventMessageData(ServerEvent.ServerOpen))
                }
                socketClient.tryOpenConnection()
            },
            onDisable = {
                runBlocking {
                    pluginBridgeApi.broadcastEvent(ServerEventMessageData(ServerEvent.ServerClosed))
                    socketClient.close()
                }
            }
        )
    }
}
