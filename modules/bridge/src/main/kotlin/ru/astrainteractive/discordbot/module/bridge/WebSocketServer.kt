package ru.astrainteractive.discordbot.module.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessageFormat
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

    override fun onOpen(client: WebSocket, handshake: ClientHandshake) {
        scope.launch {
            info { "#onOpen new client ${client.remoteSocketAddress.address.hostAddress}. Total ${connections.size}" }
        }
    }

    override fun onClose(client: WebSocket, code: Int, reason: String, remote: Boolean) {
        info { "#onClose $client has left the room!" }
    }

    override fun onMessage(client: WebSocket, text: String) {
        info { "#onMessage $client $text" }
        val decodedMessage = kotlin.runCatching {
            SocketMessageFormat.decodeFromString<SocketMessage>(text)
        }.getOrNull() ?: kotlin.runCatching {
            SocketMessageFormat.decodeFromString<SocketMessage.Data>(text)
        }.getOrThrow()
        scope.launch { _messageFlow.emit(decodedMessage) }
    }

    override fun onMessage(client: WebSocket, message: ByteBuffer) {
        info { "#onMessage $client $message but not supported" }
    }

    override fun onError(client: WebSocket?, ex: Exception) {
        info {
            "#onError ${client?.remoteSocketAddress?.address?.hostAddress}." +
                " message: ${ex.message} cause: ${ex.cause?.message}"
        }
        if (client != null) Unit
    }

    override fun onStart() {
        info { "#onStart $address" }
        connectionLostTimeout = 5
    }
}
