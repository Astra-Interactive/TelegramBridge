package ru.astrainteractive.discordbot.module.bridge.di

import kotlinx.coroutines.runBlocking
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.WebSocketClient
import ru.astrainteractive.discordbot.module.bridge.api.BridgeApi
import ru.astrainteractive.discordbot.module.bridge.api.internal.ClientBridgeApi
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

interface ClientBridgeModule {
    val clientBridgeApi: BridgeApi

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

        override val lifecycle: Lifecycle = Lifecycle.Lambda(
            onEnable = {
                runBlocking {
                    clientBridgeApi.broadcastEvent(ServerEventMessageData(ServerEvent.ServerOpen))
                }
                socketClient.tryOpenConnection()
            },
            onDisable = {
                runBlocking {
                    clientBridgeApi.broadcastEvent(ServerEventMessageData(ServerEvent.ServerClosed))
                    socketClient.close()
                }
            }
        )
    }
}
