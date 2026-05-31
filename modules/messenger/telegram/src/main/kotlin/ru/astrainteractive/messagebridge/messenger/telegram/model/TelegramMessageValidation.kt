package ru.astrainteractive.messagebridge.messenger.telegram.model

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
