package ru.astrainteractive.messagebridge.messenger.telegram.connection.fake

import org.telegram.telegrambots.longpolling.interfaces.BackOff
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

internal class FakeBackOff(
    private val interval: Duration
) : BackOff {
    val resets = AtomicInteger()
    val intervalsGiven = AtomicInteger()

    override fun reset() {
        resets.incrementAndGet()
    }

    override fun nextBackOffMillis(): Long {
        intervalsGiven.incrementAndGet()
        return interval.inWholeMilliseconds
    }
}
