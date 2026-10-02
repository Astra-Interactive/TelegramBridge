package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

internal class TelegramOnboarding(
    override val status: StateFlow<MessengerStatus>,
    override val deliveryError: StateFlow<LocalizableComponent?>,
    private val bindCodes: BindCodes,
) : MessengerOnboarding {
    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)
}
