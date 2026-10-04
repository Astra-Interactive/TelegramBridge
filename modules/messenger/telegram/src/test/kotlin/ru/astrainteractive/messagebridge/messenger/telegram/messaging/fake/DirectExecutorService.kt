package ru.astrainteractive.messagebridge.messenger.telegram.messaging.fake

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

internal object DirectExecutorService : AbstractExecutorService() {
    override fun execute(command: Runnable) {
        command.run()
    }

    override fun shutdown() = Unit

    override fun shutdownNow(): List<Runnable> = emptyList()

    override fun isShutdown(): Boolean = false

    override fun isTerminated(): Boolean = false

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
}
