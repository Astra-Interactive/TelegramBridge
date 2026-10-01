package ru.astrainteractive.messagebridge.onboarding.api.model

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

sealed interface MessengerStatus {
    data object Disabled : MessengerStatus

    data object Connecting : MessengerStatus

    data class Connected(val botName: String) : MessengerStatus

    data class Failed(val reason: LocalizableComponent) : MessengerStatus
}
