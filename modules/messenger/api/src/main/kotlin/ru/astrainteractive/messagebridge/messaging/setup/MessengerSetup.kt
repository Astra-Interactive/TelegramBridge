package ru.astrainteractive.messagebridge.messaging.setup

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/** What the setup commands need from a messenger. */
interface MessengerSetup {
    val status: StateFlow<MessengerStatus>

    /** Why the last message could not be delivered, `null` after a message is delivered. */
    val deliveryError: StateFlow<LocalizableComponent?>

    /**
     * Creates a one-time code that binds the chat it is sent to. [onBound] receives the text to tell the one who
     * asked for the code.
     */
    fun issueBindCode(onBound: (LocalizableComponent) -> Unit): String

    /** Checks the token, the chat and the rights of the bot, and sends a test message. */
    suspend fun diagnose(): List<DiagnosticCheck>
}

interface DiscordSetup : MessengerSetup {
    /** Link that adds the bot to a server with the permissions it needs, `null` until the bot is connected. */
    suspend fun inviteUrl(): String?
}
