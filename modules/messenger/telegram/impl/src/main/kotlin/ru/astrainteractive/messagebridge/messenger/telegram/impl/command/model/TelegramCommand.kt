package ru.astrainteractive.messagebridge.messenger.telegram.impl.command.model

internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand

    data class Link(val code: Int) : TelegramCommand
}
