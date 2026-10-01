package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.bind.BindCode
import ru.astrainteractive.messagebridge.onboarding.bind.BindCodes
import ru.astrainteractive.messagebridge.onboarding.check.Check
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus

internal class TelegramOnboarding(
    override val status: StateFlow<MessengerStatus>,
    override val deliveryError: StateFlow<LocalizableComponent?>,
    private val bindCodes: BindCodes,
    private val diagnostics: TelegramDiagnostics,
) : MessengerOnboarding {
    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)

    override suspend fun check(): List<Check> = diagnostics.diagnose()
}
