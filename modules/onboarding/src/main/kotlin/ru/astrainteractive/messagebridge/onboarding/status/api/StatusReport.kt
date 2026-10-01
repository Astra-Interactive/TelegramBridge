package ru.astrainteractive.messagebridge.onboarding.status.api

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

internal interface StatusReport {
    fun lines(): List<LocalizableComponent>
}
