package ru.astrainteractive.messagebridge.onboarding.setting

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/**
 * A change of one messenger's part [C] of config.yml.
 *
 * @property saved the reply once the change is written
 */
internal class Setting<C>(
    val saved: LocalizableComponent,
    val change: (C) -> C
)
