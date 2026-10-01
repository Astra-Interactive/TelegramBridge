package ru.astrainteractive.messagebridge.onboarding.telegram.fake

import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel

internal data class ShownCheck(
    val level: CheckLevel,
    val text: String
)
