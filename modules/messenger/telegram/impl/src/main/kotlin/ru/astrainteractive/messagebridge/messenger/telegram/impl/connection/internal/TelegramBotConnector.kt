package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.telegram.telegrambots.longpolling.interfaces.BackOff
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.api.TelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.impl.mapping.TelegramFailureMapper
import kotlin.time.Duration.Companion.milliseconds

internal class TelegramBotConnector(
    private val sessionFactory: (TelegramConnection.Ready) -> TelegramBotSession,
    private val backOffFactory: () -> BackOff,
    private val failureMapper: TelegramFailureMapper,
) : Logger by JUtiltLogger("MessageBridge-TelegramConnector") {
    private val mutableState = MutableStateFlow<TelegramConnectionState>(TelegramConnectionState.Connecting)
    val state: StateFlow<TelegramConnectionState> = mutableState.asStateFlow()

    private val botUserNameMutex = Mutex()
    private var knownBotUserName: String? = null

    private suspend fun updateBotUserName(userName: String?) {
        botUserNameMutex.withLock { knownBotUserName = userName }
    }

    private suspend fun runOnce(session: TelegramBotSession, backOff: BackOff): TelegramFailure {
        val userName = session.fetchBotUserName().getOrElse { throwable -> return failureMapper.map(throwable) }
        val botName = "@$userName"
        updateBotUserName(userName)
        mutableState.value = TelegramConnectionState.Connected(botName)
        val polling = session.startPolling(botName, onState = { state -> mutableState.value = state })
            .getOrElse { throwable -> return failureMapper.map(throwable) }
        backOff.reset()
        verbose { "#runOnce the bot is polling" }
        polling.use { _ -> awaitCancellation() }
    }

    private suspend fun keepRunning(connection: TelegramConnection.Ready): Nothing {
        val session = sessionFactory(connection)
        val backOff = backOffFactory()
        mutableState.value = TelegramConnectionState.Connecting
        while (true) {
            val failure = runOnce(session, backOff)
            mutableState.value = TelegramConnectionState.Failed(failure)
            if (failure.needsNewSettings) awaitCancellation()
            val retryDelay = backOff.nextBackOffMillis().milliseconds
            verbose { "#keepRunning could not connect, retrying in $retryDelay: $failure" }
            delay(retryDelay)
        }
    }

    suspend fun botUserName(): String? {
        return botUserNameMutex.withLock { knownBotUserName }
    }

    suspend fun connect(connections: Flow<TelegramConnection>) {
        connections.collectLatest { connection ->
            updateBotUserName(null)
            when (connection) {
                TelegramConnection.Disabled -> mutableState.value = TelegramConnectionState.Disabled
                is TelegramConnection.Invalid -> mutableState.value = TelegramConnectionState.Failed(connection.failure)
                is TelegramConnection.Ready -> keepRunning(connection)
            }
        }
    }
}
