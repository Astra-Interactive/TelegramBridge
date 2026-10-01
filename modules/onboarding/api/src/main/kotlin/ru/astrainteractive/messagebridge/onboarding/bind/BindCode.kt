package ru.astrainteractive.messagebridge.onboarding.bind

import kotlin.time.Duration

/** @property lifetime how long [value] can be sent to bind a chat */
data class BindCode(
    val value: String,
    val lifetime: Duration
)
