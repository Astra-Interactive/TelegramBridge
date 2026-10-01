package ru.astrainteractive.messagebridge.onboarding.api.model

import kotlin.time.Duration

data class BindCode(
    val value: String,
    val lifetime: Duration
)
