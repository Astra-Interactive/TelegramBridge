package ru.astrainteractive.messagebridge.messenger.telegram.connection

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import org.telegram.telegrambots.longpolling.interfaces.BackOff
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailureMapper
import kotlin.time.Duration.Companion.milliseconds

/**
 * Keeps the bot of the latest connection running. The token is checked with getMe before polling, so a wrong one is
 * reported once and waits for new settings instead of being retried forever.
 */
internal class TelegramBotConnector(
    private val sessionFactory: (TelegramConnection.Ready) -> TelegramBotSession,
    private val backOffFactory: () -> BackOff,
    private val failureMapper: TelegramFailureMapper,
    logger: Logger,
) : Logger by logger {
    private val mutableState = MutableStateFlow<TelegramConnectionState>(TelegramConnectionState.Connecting)
    val state: StateFlow<TelegramConnectionState> = mutableState.asStateFlow()

    /** Username of the running bot without `@`, `null` until Telegram accepts the token. */
    @Volatile
    var botUserName: String? = null
        private set

    /** @return why the bot stopped; it runs until cancelled once the polling has started */
    private suspend fun runOnce(session: TelegramBotSession, backOff: BackOff): TelegramFailure {
        val userName = session.fetchBotUserName().getOrElse { throwable -> return failureMapper.map(throwable) }
        val botName = "@$userName"
        botUserName = userName
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

    /** Suspends until cancelled, following every new connection. */
    suspend fun connect(connections: Flow<TelegramConnection>) {
        connections.collectLatest { connection ->
            botUserName = null
            when (connection) {
                TelegramConnection.Disabled -> mutableState.value = TelegramConnectionState.Disabled
                is TelegramConnection.Invalid -> mutableState.value = TelegramConnectionState.Failed(connection.failure)
                is TelegramConnection.Ready -> keepRunning(connection)
            }
        }
    }
}
