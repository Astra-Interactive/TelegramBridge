package ru.astrainteractive.messagebridge.messenger.telegram.model

internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand

    data class Link(val code: Int) : TelegramCommand
}
