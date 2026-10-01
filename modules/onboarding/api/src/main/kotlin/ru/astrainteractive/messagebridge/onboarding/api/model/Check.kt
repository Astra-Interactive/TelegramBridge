package ru.astrainteractive.messagebridge.onboarding.api.model

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

data class Check(
    val level: CheckLevel,
    val message: LocalizableComponent
)
