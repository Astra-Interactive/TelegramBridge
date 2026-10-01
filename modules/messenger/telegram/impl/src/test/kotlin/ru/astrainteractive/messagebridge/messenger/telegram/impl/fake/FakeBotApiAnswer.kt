package ru.astrainteractive.messagebridge.messenger.telegram.impl.fake

import kotlin.time.Duration

internal data class FakeBotApiAnswer(
    val code: Int,
    val body: String,
    val delay: Duration
)
