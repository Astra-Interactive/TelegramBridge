package ru.astrainteractive.messagebridge.messenger.telegram.message.model

internal sealed interface TelegramAuthor {
    val name: String

    data class Username(override val name: String) : TelegramAuthor

    data class DisplayName(override val name: String) : TelegramAuthor
}
