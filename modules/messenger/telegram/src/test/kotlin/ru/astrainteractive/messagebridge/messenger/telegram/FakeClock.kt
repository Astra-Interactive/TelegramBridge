package ru.astrainteractive.messagebridge.messenger.telegram

import kotlin.time.Clock
import kotlin.time.Instant

internal class FakeClock(
    @Volatile var now: Instant
) : Clock {
    override fun now(): Instant = now
}
