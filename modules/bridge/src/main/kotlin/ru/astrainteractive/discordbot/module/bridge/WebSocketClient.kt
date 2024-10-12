package ru.astrainteractive.discordbot.module.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import ru.astrainteractive.astralibs.async.CoroutineFeature
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageFactory
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageFormat
import ru.astrainteractive.discordbot.module.bridge.serializer.SocketMessageSerializer
import ru.astrainteractive.discordbot.module.bridge.serializer.send
import ru.astrainteractive.discordbot.module.bridge.util.AtomicList
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Suppress("TooManyFunctions")
internal class WebSocketClient(
    private val host: String,
    private val port: Int
) : WebSocketListener(),
    Logger by JUtiltLogger("WebSocketClient") {

    private val client: OkHttpClient
        get() = OkHttpClient.Builder()
            .readTimeout(RECONNECT_DELAY.div(2).toJavaDuration())
            .build()

    private val request: Request
        get() = Request.Builder()
            .url("ws://$host:$port")
            .build()

    private val scope = CoroutineFeature.Default(Dispatchers.IO)

    private val mutex = Mutex()

    private val webSocketFlow: MutableStateFlow<WebSocket?> = MutableStateFlow(null)

    private val pendingMessages = AtomicList<SocketMessage>()

    private val counter = AtomicLong(0)

    private val _messageFlow = MutableSharedFlow<SocketMessage>()
    val messageFlow = _messageFlow.asSharedFlow()

    suspend fun awaitConnected() = webSocketFlow.filterNotNull().first()

    private suspend fun send(
        message: SocketMessage,
    ): Flow<SocketMessage> {
        pendingMessages.add(message)
        val socket = webSocketFlow.value
        val encoded = SocketMessageFormat.encodeToString(message)
        socket?.send(encoded)
        return messageFlow.filter { it.id == message.id }
    }

    suspend fun <T> send(route: SocketRoute, data: T? = null): Flow<SocketMessage> {
        info { "#send $route" }
        val message = SocketMessageFactory.create(
            data = data,
            route = route,
            getId = { counter.incrementAndGet() }
        )
        return send(message)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        info { "#onOpen: $webSocket" }
        webSocketFlow.update { webSocket }
        scope.launch {
            pendingMessages.removeWhere {
                Clock.System.now().minus(it.created) > MAX_MESSAGE_LIFETIME
            }
            pendingMessages
                .toList()
                .forEach { webSocket.send(it) }
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        scope.launch {
            info { "#onMessage: $webSocket $text" }
            val decodedMessage = SocketMessageSerializer.fromString(text)
            val removed = pendingMessages.removeWhere { listMessage -> listMessage.id == decodedMessage.id }
            if (removed != null) counter.decrementAndGet()
            _messageFlow.emit(decodedMessage)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        info { "#onMessage: $webSocket ${bytes.hex()} but not supported" }
    }

    fun tryOpenConnection() = scope.launch {
        info { "#openConnection" }
        if (webSocketFlow.value != null) return@launch
        mutex.withLock {
            kotlin.runCatching { client.newWebSocket(request, this@WebSocketClient) }
                .onFailure { error { "#openConnection ${it.message} ${it.cause?.message}" } }
                .getOrNull()
        }
    }

    private suspend fun reconnectIfNeed(code: Int?) {
        if (code == EXIT_CODE) return
        scope.launch {
            delay(RECONNECT_DELAY)
            tryOpenConnection().join()
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        info { "#onClosing: $webSocket $code $reason" }
        scope.launch {
            mutex.withLock {
                webSocket.cancel()
                webSocketFlow.update { null }
            }
            reconnectIfNeed(code)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        info { "#onFailure: $webSocket ${t.message} ${t.cause?.message}" }
        scope.launch {
            mutex.withLock {
                webSocket.cancel()
                webSocketFlow.update { null }
            }
            reconnectIfNeed(response?.code)
        }
    }

    suspend fun close() {
        info { "#close" }
        mutex.withLock {
            val socket = webSocketFlow.getAndUpdate { null }
            socket?.close(EXIT_CODE, null)
            socket?.cancel()
            client.dispatcher.cancelAll()
            client.dispatcher.executorService.shutdown()
            scope.cancel()
        }
    }

    companion object {
        private const val EXIT_CODE = 4999
        private val RECONNECT_DELAY = 5.seconds
        private val MAX_MESSAGE_LIFETIME = 30.seconds
    }
}
