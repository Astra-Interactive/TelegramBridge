package ru.astrainteractive.messagebridge.onboarding

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.bind.BindCode
import ru.astrainteractive.messagebridge.onboarding.check.Check
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus

/** What the /mb commands need from a messenger to set it up. */
interface MessengerOnboarding {
    val status: StateFlow<MessengerStatus>

    /** Why the last message could not be delivered, `null` after a message is delivered. */
    val deliveryError: StateFlow<LocalizableComponent?>

    /**
     * Creates a one-time code that binds the chat it is sent to.
     *
     * @param onBound receives the text for the one who asked for the code, once the chat is bound
     */
    fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode

    /** Checks the token, the chat and the rights of the bot, and sends a test message. */
    suspend fun check(): List<Check>
}
