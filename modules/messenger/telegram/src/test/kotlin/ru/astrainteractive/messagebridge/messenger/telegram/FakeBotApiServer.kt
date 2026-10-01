package ru.astrainteractive.messagebridge.messenger.telegram

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.telegram.telegrambots.meta.TelegramUrl
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import kotlin.time.Duration

/** A Bot API server on localhost that answers every method with the JSON a test gives it. */
internal class FakeBotApiServer : AutoCloseable {
    private val answers = ConcurrentHashMap<String, FakeBotApiAnswer>()
    private val executor = Executors.newCachedThreadPool()
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

    /** Methods in lower case, in the order they are requested. */
    val requestedMethods: MutableList<String> = CopyOnWriteArrayList()

    val url: TelegramUrl
        get() = TelegramUrl("http", server.address.hostString, server.address.port, false)

    val apiUrl: String
        get() = "http://${server.address.hostString}:${server.address.port}"

    private fun handle(exchange: HttpExchange) {
        val method = exchange.requestURI.path.substringAfterLast('/').lowercase()
        requestedMethods += method
        exchange.requestBody.use { body -> body.readBytes() }
        val answer = answers[method] ?: FakeBotApiAnswer(code = NOT_FOUND, body = NOT_FOUND_BODY, delay = Duration.ZERO)
        Thread.sleep(answer.delay.inWholeMilliseconds)
        val bytes = answer.body.toByteArray()
        exchange.sendResponseHeaders(answer.code, bytes.size.toLong())
        exchange.responseBody.use { body -> body.write(bytes) }
    }

    init {
        server.createContext("/") { exchange -> handle(exchange) }
        server.executor = executor
        server.start()
    }

    fun answer(method: String, code: Int, body: String, delay: Duration = Duration.ZERO) {
        answers[method.lowercase()] = FakeBotApiAnswer(code = code, body = body, delay = delay)
    }

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private companion object {
        const val NOT_FOUND = 404
        const val NOT_FOUND_BODY = """{"ok":false,"error_code":404,"description":"Not Found"}"""
    }
}
