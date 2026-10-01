package ru.astrainteractive.messagebridge.messenger.telegram.connection.fake

import ru.astrainteractive.messagebridge.messenger.telegram.connection.api.TelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.connection.model.TelegramConnectionState
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

internal class FakeTelegramBotSession(
    userNameResults: List<Result<String>>,
    pollingResults: List<Result<Unit>>,
    private val userName: String,
) : TelegramBotSession {
    private val userNameResults = ConcurrentLinkedQueue(userNameResults)
    private val pollingResults = ConcurrentLinkedQueue(pollingResults)

    val fetches = AtomicInteger()
    val pollings: MutableList<FakePolling> = CopyOnWriteArrayList()

    @Volatile
    var reportState: (TelegramConnectionState) -> Unit = { _ -> }
        private set

    override suspend fun fetchBotUserName(): Result<String> {
        fetches.incrementAndGet()
        return userNameResults.poll() ?: Result.success(userName)
    }

    override suspend fun startPolling(
        botName: String,
        onState: (TelegramConnectionState) -> Unit
    ): Result<AutoCloseable> {
        reportState = onState
        val result = pollingResults.poll() ?: Result.success(Unit)
        return result.map { _ -> FakePolling().also(pollings::add) }
    }
}
