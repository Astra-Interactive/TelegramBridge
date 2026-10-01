package ru.astrainteractive.messagebridge.onboarding.status

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/** What /mb status shows about one messenger: the bot, where it writes and how it connects. */
internal interface StatusReport {
    fun lines(): List<LocalizableComponent>
}
