package ru.astrainteractive.messagebridge.messenger.telegram.connection

import okhttp3.OkHttpClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

/** One run of the getUpdates polling; closing it releases everything the run has started. */
internal class LongPolling(
    private val statusInterceptor: GetUpdatesStatusInterceptor,
    private val pollingClient: OkHttpClient,
    private val pollerExecutor: ExecutorService,
    private val application: TelegramBotsLongPollingApplication,
    logger: Logger,
) : AutoCloseable,
    Logger by logger {
    /** Blocks until Telegram accepts the bot, then polls in the background. */
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
