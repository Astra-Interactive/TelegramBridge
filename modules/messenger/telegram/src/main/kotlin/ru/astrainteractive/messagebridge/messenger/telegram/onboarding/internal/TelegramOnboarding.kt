package ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

internal class TelegramOnboarding(
    override val status: StateFlow<MessengerStatus>,
    override val deliveryError: StateFlow<LocalizableComponent?>,
    private val bindCodes: BindCodes,
    private val diagnostics: TelegramDiagnostics,
) : MessengerOnboarding {
    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)

    override suspend fun check(): List<Check> = diagnostics.diagnose()
}
