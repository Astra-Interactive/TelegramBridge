package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import okhttp3.Dispatcher
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.interfaces.BackOff
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.GetMe
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.api.TelegramBotSession
import ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping.TelegramFailureMapper
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

internal class LongPollingBotSession(
    private val connection: TelegramConnection.Ready,
    private val updateConsumer: LongPollingUpdateConsumer,
    private val failureMapper: TelegramFailureMapper,
    private val backOffFactory: () -> BackOff,
    private val timeSource: TimeSource,
    private val logger: Logger,
) : TelegramBotSession {
    private fun createLongPolling(botName: String, onState: (TelegramConnectionState) -> Unit): LongPolling {
        val statusInterceptor = GetUpdatesStatusInterceptor(
            botName = botName,
            failureMapper = failureMapper,
            timeSource = timeSource,
            onState = onState
        )
        val pollingClient = connection.okHttpClient.newBuilder()
            .dispatcher(Dispatcher())
            .addInterceptor(statusInterceptor)
            .build()
        val pollerExecutor = Executors.newSingleThreadScheduledExecutor()
        val application = TelegramBotsLongPollingApplication(
            Supplier(::ObjectMapper),
            Supplier { pollingClient },
            Supplier { pollerExecutor },
            Supplier { backOffFactory() }
        )
        return LongPolling(
            statusInterceptor = statusInterceptor,
            pollingClient = pollingClient,
            pollerExecutor = pollerExecutor,
            application = application,
            logger = logger
        )
    }

    override suspend fun fetchBotUserName(): Result<String> {
        return runCatching { connection.telegramClient.executeAsync(GetMe()).await().userName }
            .onFailure { throwable -> if (throwable is CancellationException) throw throwable }
    }

    override suspend fun startPolling(
        botName: String,
        onState: (TelegramConnectionState) -> Unit
    ): Result<AutoCloseable> {
        val polling = createLongPolling(botName, onState)
        return runCatching {
            runInterruptible {
                polling.register(connection, updateConsumer)
            }
        }.onFailure { throwable ->
            polling.close()
            if (throwable is CancellationException) throw throwable
        }.map { _ -> polling }
    }
}
