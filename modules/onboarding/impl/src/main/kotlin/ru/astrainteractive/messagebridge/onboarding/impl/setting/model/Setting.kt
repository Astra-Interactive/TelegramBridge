package ru.astrainteractive.messagebridge.onboarding.impl.setting.model

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

internal class Setting<C>(
    val saved: LocalizableComponent,
    val change: (C) -> C
)
