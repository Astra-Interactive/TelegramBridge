package ru.astrainteractive.messagebridge.onboarding.bind

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import kotlin.time.Instant

internal class BindRequest(
    val expiresAt: Instant,
    val onBound: (LocalizableComponent) -> Unit
)
