package ru.astrainteractive.messagebridge.messenger.telegram.model

/**
 * The author of a Telegram message. Whether the author has a public `@username` changes how the
 * display name is validated, so the distinction is modeled in the type rather than a boolean flag.
 */
internal sealed interface TelegramAuthor {
    val name: String

    /** Author identified by a public `@username`. */
    data class Username(override val name: String) : TelegramAuthor

    /** Author without a `@username`, identified only by their first/last name. */
    data class DisplayName(override val name: String) : TelegramAuthor
}