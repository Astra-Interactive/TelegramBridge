package ru.astrainteractive.messagebridge.messenger.telegram.onboarding.fake

import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel

internal data class ShownCheck(
    val level: CheckLevel,
    val text: String
)
