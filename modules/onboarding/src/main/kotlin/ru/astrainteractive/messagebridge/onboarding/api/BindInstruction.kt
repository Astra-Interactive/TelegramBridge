package ru.astrainteractive.messagebridge.onboarding.api

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode

internal interface BindInstruction {
    fun of(code: BindCode): LocalizableComponent
}
