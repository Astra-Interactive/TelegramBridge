package ru.astrainteractive.messagebridge.messaging.setup

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/** One line of a messenger check: what was checked and, when it failed, what to do. */
data class DiagnosticCheck(
    val level: Level,
    val message: LocalizableComponent
) {
    enum class Level { OK, WARNING, ERROR }
}
