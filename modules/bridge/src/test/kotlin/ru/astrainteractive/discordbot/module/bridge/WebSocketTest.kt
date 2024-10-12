package ru.astrainteractive.discordbot.module.bridge

import app.cash.turbine.test
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.discordbot.module.bridge.model.SocketBotMessageReceivedMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketPongMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRequestOnlineListMessage
import ru.astrainteractive.discordbot.module.bridge.model.SocketRoute
import ru.astrainteractive.discordbot.module.bridge.model.data.BotMessageReceivedMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.ServerEventMessageData
import ru.astrainteractive.discordbot.module.bridge.model.data.UpdateOnlineMessageData
import ru.astrainteractive.messagebridge.messaging.model.ServerEvent
import java.net.InetAddress
import java.util.UUID
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
        requireContext.socketClient.send(SocketRoute.UPDATE_ONLINE, UpdateOnlineMessageData(10, 30)).test {
            assert(awaitItem() is SocketPongMessage)
        }
        // Bot received discord message
        requireContext.socketClient.messageFlow.test {
            requireContext.socketServer.broadcast(
                SocketRoute.BOT_MESSAGE_RECEIVED,
                BotMessageReceivedMessageData(
                    message = "Hello world",
                    fromUserId = "0"
                )
            )
            assert(awaitItem() is SocketBotMessageReceivedMessage)
        }
        // Request online
        requireContext.socketClient.messageFlow.test {
            requireContext.socketServer.broadcast<Nothing>(SocketRoute.REQUEST_ONLINE_LIST)
            assert(awaitItem() is SocketRequestOnlineListMessage)
        }
        // Test event
        requireContext.socketClient.messageFlow.test {
            requireContext.socketClient.send(
                SocketRoute.MESSAGE,
                ServerEventMessageData(
                    instance = ServerEvent.Text.Minecraft(
                        author = "author",
                        text = "text",
                        uuid = UUID.randomUUID().toString()
                    )
                )
            )
            assert(awaitItem().also { println(it) } is SocketPongMessage)
        }
    }
}
