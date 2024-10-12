package ru.astrainteractive.discordbot.module.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.broadcast
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageFactory
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageSerializer
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
            info {
                """
                 #onOpen new client ${client.remoteSocketAddress.address.hostAddress}.\n
                 Total connections: ${connections.size}
                """.trimIndent().replace("\\n", "")
            }
        }
    }

    override fun onClose(client: WebSocket, code: Int, reason: String, remote: Boolean) {
        info { "#onClose $client has left the room!" }
    }

    override fun onMessage(client: WebSocket, text: String) {
        info { "#onMessage $client $text" }
        val decodedMessage = SocketMessageSerializer.fromString(text)
        scope.launch { _messageFlow.emit(decodedMessage) }
    }

    override fun onMessage(client: WebSocket, message: ByteBuffer) {
        info { "#onMessage $client $message but not supported" }
    }

    override fun onError(client: WebSocket?, ex: Exception) {
        info {
            """
             #onError ${client?.remoteSocketAddress?.address?.hostAddress}.\n
             message: ${ex.message} cause: ${ex.cause?.message}
            """.trimIndent().replace("\\n", "")
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
