package ru.astrainteractive.discordbot.module.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.model.SocketBotMessageReceivedMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPingMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRequestOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.SocketServerEventMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketUpdateOnlineMessage
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageFactory
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageSerializer
import ru.astrainteractive.discordbot.module.bridge.serializer.broadcast
import java.net.InetSocketAddress
import java.nio.ByteBuffer

internal class WebSocketServer(
    host: String,
    port: Int
) : WebSocketServer(InetSocketAddress(host, port)),
    Logger by JUtiltLogger("ChatServer") {
    private val scope = CoroutineFeature.Default(Dispatchers.IO)

    private val _messageFlow = MutableSharedFlow<SocketMessage>()
    val messageFlow = _messageFlow.asSharedFlow()

    private val isOpen = MutableStateFlow(false)

    suspend fun awaitOpen() = isOpen.filter { true }.first()

    override fun onOpen(client: WebSocket, handshake: ClientHandshake) {
        isOpen.update { true }
        scope.launch {
            info {
                @Suppress("MaxLineLength")
                "#onOpen new client ${client.remoteSocketAddress.address.hostAddress}. Total connections: ${connections.size}"
            }
        }
    }

    override fun onClose(client: WebSocket, code: Int, reason: String, remote: Boolean) {
        isOpen.update { false }
        info { "#onClose $client has left the room!" }
    }

    override fun onMessage(client: WebSocket, text: String) {
        info { "#onMessage $client $text" }
        val decodedMessage = SocketMessageSerializer.fromString(text)
        scope.launch {
            _messageFlow.emit(decodedMessage)
            when (decodedMessage) {
                is SocketOnlineListMessage,
                is SocketUpdateOnlineMessage,
                is SocketServerEventMessage,
                is SocketPingMessage,
                is SocketPongMessage -> {
                    val response = SocketPongMessage(decodedMessage.id)
                    broadcast(response)
                }

                is SocketRequestOnlineListMessage,
                is SocketBotMessageReceivedMessage -> {
                    error { "#onMessage ${decodedMessage::class} is not for parsing" }
                }
            }
        }
    }

    override fun onMessage(client: WebSocket, message: ByteBuffer) {
        info { "#onMessage $client $message but not supported" }
    }

    override fun onError(client: WebSocket?, ex: Exception) {
        info {
            @Suppress("MaxLineLength")
            "#onError ${client?.remoteSocketAddress?.address?.hostAddress}.message: ${ex.message} cause: ${ex.cause?.message}"
        }
    }

    suspend fun <T> broadcast(route: SocketRoute, data: T? = null) {
        val message = SocketMessageFactory.create(
            route = route,
            data = data,
            getId = { -1 }
        )
        broadcast(message)
    }

    override fun onStart() {
        info { "#onStart $address" }
        connectionLostTimeout = 5
    }
}
