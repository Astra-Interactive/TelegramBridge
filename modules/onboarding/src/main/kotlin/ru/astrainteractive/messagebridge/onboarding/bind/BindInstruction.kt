package ru.astrainteractive.messagebridge.onboarding.bind

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/** Tells the admin what to send into the chat of a messenger to bind it with a code. */
internal interface BindInstruction {
    fun of(code: BindCode): LocalizableComponent
}
