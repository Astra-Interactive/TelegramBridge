package ru.astrainteractive.messagebridge.onboarding.check

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/** One line of a messenger check: what was checked and, when it failed, what to do. */
data class Check(
    val level: CheckLevel,
    val message: LocalizableComponent
)
