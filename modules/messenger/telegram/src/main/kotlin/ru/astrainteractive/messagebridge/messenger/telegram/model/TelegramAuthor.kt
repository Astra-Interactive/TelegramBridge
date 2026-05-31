package ru.astrainteractive.messagebridge.messenger.telegram.model

internal sealed interface TelegramAuthor {
    val name: String

    /** Author identified by a public `@username`. */
    data class Username(override val name: String) : TelegramAuthor

    /** Author without a `@username`, identified only by their first/last name. */
    data class DisplayName(override val name: String) : TelegramAuthor
}
