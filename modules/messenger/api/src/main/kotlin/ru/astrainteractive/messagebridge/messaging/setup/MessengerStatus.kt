package ru.astrainteractive.messagebridge.messaging.setup

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent

/** State of the connection of a messenger bot. */
sealed interface MessengerStatus {
    /** The bot token is empty, so the messenger is turned off. */
    data object Disabled : MessengerStatus

    data object Connecting : MessengerStatus

    /** @param botName name of the bot as the messenger shows it, e.g. `@MyServerBot` */
    data class Connected(val botName: String) : MessengerStatus

    /** @param reason what went wrong and what to do about it */
    data class Failed(val reason: LocalizableComponent) : MessengerStatus
}
