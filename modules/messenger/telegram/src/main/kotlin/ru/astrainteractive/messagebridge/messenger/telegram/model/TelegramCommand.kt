package ru.astrainteractive.messagebridge.messenger.telegram.model

internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand

    data class Link(val code: Int) : TelegramCommand

    /** Works in any chat, so a chat can be found and bound before it is set up. */
    sealed interface Setup : TelegramCommand

    data class Bind(val code: String) : Setup

    data object ChatInfo : Setup
}
