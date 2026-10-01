package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import ru.astrainteractive.messagebridge.onboarding.check.CheckLevel

/** A check as the admin reads it, so two checks compare by their text. */
internal data class ShownCheck(
    val level: CheckLevel,
    val text: String
)
