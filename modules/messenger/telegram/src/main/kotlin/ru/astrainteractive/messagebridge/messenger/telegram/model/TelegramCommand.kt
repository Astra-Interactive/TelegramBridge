package ru.astrainteractive.messagebridge.messenger.telegram.model

/**
 * A chat command that consumes a message instead of relaying it to the server.
 */
internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand

    data class Link(val code: Int) : TelegramCommand
}