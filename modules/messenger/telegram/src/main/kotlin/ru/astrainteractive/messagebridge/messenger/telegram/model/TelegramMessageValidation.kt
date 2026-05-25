package ru.astrainteractive.messagebridge.messenger.telegram.model

/**
 * The verdict for a relevant message's content. Each rejection cause is its own type so the
 * caller decides how to react (e.g. reply then delete) without branching on a reason string.
 */
internal sealed interface TelegramMessageValidation {
    data class Valid(
        val author: String,
        val text: String,
        val authorId: Long,
    ) : TelegramMessageValidation

    data object NoAuthor : TelegramMessageValidation
    data object IllegalDisplayName : TelegramMessageValidation
    data object NoText : TelegramMessageValidation
    data object TooLong : TelegramMessageValidation
}