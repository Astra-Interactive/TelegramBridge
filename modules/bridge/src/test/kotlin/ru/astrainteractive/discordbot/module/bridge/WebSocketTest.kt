package ru.astrainteractive.discordbot.module.bridge

import app.cash.turbine.test
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.discordbot.module.bridge.model.SocketBotMessageReceivedMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedData
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineData
import java.net.InetAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class WebSocketTest {
    private class TestContext(
        val socketClient: WebSocketClient,
        val socketServer: WebSocketServer
    )

    private fun TestContext.prepare() = runBlocking {
        flow {
            socketServer.start()
            socketServer.awaitOpen()

            socketClient.tryOpenConnection()
            socketClient.awaitConnected()
            emit(Unit)
        }.timeout(3.seconds).collect()
    }

    private fun TestContext.close() = runBlocking {
        flow {
            socketClient.close()
            socketServer.stop(0, "Server closed")
            emit(Unit)
        }.timeout(3.seconds).collect()
    }

    private fun createContext(): TestContext {
        val host = InetAddress.getLocalHost().hostAddress
        val port = 51300
        return TestContext(
            socketClient = WebSocketClient(
                host = host,
                port = port
            ),
            socketServer = WebSocketServer(
                host = host,
                port = port
            )
        )
    }

    private var context: TestContext? = null
    private val requireContext: TestContext
        get() = context ?: error("Could not get context")

    @BeforeTest
    fun setup() {
        context = createContext()
        context?.prepare()
    }

    @AfterTest
    fun destroy() {
        context?.close()
    }

    @Test
    fun test() = runTest {
        requireContext.socketClient.send<Nothing>(SocketRoute.PING).test {
            assert(awaitItem() is SocketPongMessage)
        }
        requireContext.socketClient.send<Nothing>(SocketRoute.PONG).test {
            assert(awaitItem() is SocketPongMessage)
        }
        requireContext.socketClient.send(SocketRoute.UPDATE_ONLINE, UpdateOnlineData(10, 30)).test {
            assert(awaitItem() is SocketPongMessage)
        }
        // Bot received discord message
        requireContext.socketServer.broadcast(
            SocketRoute.BOT_MESSAGE_RECEIVED,
            BotMessageReceivedData(
                message = "Hello world",
                fromUserId = 0L
            )
        )
        requireContext.socketClient.messageFlow.test {
            assert(awaitItem() is SocketBotMessageReceivedMessage)
        }
    }
}
