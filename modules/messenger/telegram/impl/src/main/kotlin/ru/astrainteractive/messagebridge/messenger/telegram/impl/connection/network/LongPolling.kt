package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.network

import okhttp3.OkHttpClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

internal class LongPolling(
    private val statusInterceptor: GetUpdatesStatusInterceptor,
    private val pollingClient: OkHttpClient,
    private val pollerExecutor: ExecutorService,
    private val application: TelegramBotsLongPollingApplication,
) : AutoCloseable,
    Logger by JUtiltLogger("MessageBridge-TelegramPolling") {
    fun register(connection: TelegramConnection.Ready, updateConsumer: LongPollingUpdateConsumer) {
        application.registerBot(
            connection.token,
            Supplier { connection.url },
            DefaultGetUpdatesGenerator(),
            updateConsumer
        )
    }

    override fun close() {
        verbose { "#close closing TelegramBotsLongPollingApplication..." }
        statusInterceptor.deactivate()
        pollingClient.dispatcher.cancelAll()
        runCatching { application.close() }
            .onFailure { throwable -> error(throwable) { "#close could not close the polling: ${throwable.message}" } }
        pollerExecutor.shutdownNow()
        pollingClient.dispatcher.executorService.shutdown()
    }
}
