package ru.astrainteractive.discordbot.module.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
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
import ru.astrainteractive.discordbot.module.bridge.model.AtomicList
import ru.astrainteractive.discordbot.module.bridge.model.MessageData
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketMessageFormat
import ru.astrainteractive.discordbot.module.bridge.model.send
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

    fun tryOpenConnection() = scope.launch {
        info { "#openConnection" }
        if (webSocketFlow.value != null) return@launch
        mutex.withLock {
            kotlin.runCatching { client.newWebSocket(request, this@WebSocketClient) }
                .onFailure { error { "#openConnection ${it.message} ${it.cause?.message}" } }
                .getOrNull()
        }
    }

    private suspend fun send(
        message: SocketMessage,
    ): Flow<SocketMessage> {
//        pendingMessages.add(message)
        val socket = webSocketFlow.value
        val encoded = SocketMessageFormat.encodeToString(message)
        socket?.send(encoded)
        return messageFlow.filter { it.id == message.id }
    }

    suspend fun send(data: MessageData): Flow<SocketMessage> {
        val message = SocketMessage.Data(
            id = counter.incrementAndGet(),
            data = data
        )
        return send(message)
    }

    suspend fun ping() = send(SocketMessage.Ping(counter.incrementAndGet()))

    override fun onOpen(webSocket: WebSocket, response: Response) {
        info { "#onOpen: $webSocket" }
        webSocketFlow.update { webSocket }
        pendingMessages
            .toList()
            .forEach { webSocket.send(it) }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        scope.launch {
            info { "#onMessage: $webSocket $text" }
            val decodedMessage = kotlin.runCatching {
                SocketMessageFormat.decodeFromString<SocketMessage>(text)
            }.getOrNull() ?: kotlin.runCatching {
                SocketMessageFormat.decodeFromString<SocketMessage.Data>(text)
            }.getOrThrow()
            val removed = pendingMessages.removeWhere { listMessage -> listMessage.id == decodedMessage.id }
            if (removed != null) counter.decrementAndGet()
            _messageFlow.emit(decodedMessage)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        info { "#onMessage: $webSocket ${bytes.hex()} but not supported" }
    }

    private suspend fun reconnectIfNeed(code: Int?) {
        if (code != EXIT_CODE) {
            scope.launch {
                delay(RECONNECT_DELAY)
                tryOpenConnection()
            }
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
            webSocketFlow.value?.close(EXIT_CODE, null)
            webSocketFlow.value?.cancel()
            client.dispatcher.cancelAll()
            client.dispatcher.executorService.shutdown()
            webSocketFlow.update { null }
            scope.cancel()
        }
    }

    companion object {
        private const val EXIT_CODE = -222
        private val RECONNECT_DELAY = 5.seconds
    }
}
