package ru.astrainteractive.messagebridge.messenger.telegram

import kotlin.time.Duration

/** @property delay how long the server holds the answer, the way getUpdates waits for new messages */
internal data class FakeBotApiAnswer(
    val code: Int,
    val body: String,
    val delay: Duration
)
