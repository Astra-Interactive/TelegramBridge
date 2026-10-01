package ru.astrainteractive.messagebridge.onboarding.api.api

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

interface MessengerOnboarding {
    val status: StateFlow<MessengerStatus>

    val deliveryError: StateFlow<LocalizableComponent?>

    fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode

    suspend fun check(): List<Check>
}
