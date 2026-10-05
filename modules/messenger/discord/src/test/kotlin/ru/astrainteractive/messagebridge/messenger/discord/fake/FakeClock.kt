package ru.astrainteractive.messagebridge.messenger.discord.fake

import kotlin.time.Clock
import kotlin.time.Instant

internal class FakeClock(var current: Instant) : Clock {
    override fun now(): Instant = current
}
