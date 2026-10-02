package ru.astrainteractive.messagebridge.core.api.fake

import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(
    var now: Instant
) : Clock {
    override fun now(): Instant = now
}
