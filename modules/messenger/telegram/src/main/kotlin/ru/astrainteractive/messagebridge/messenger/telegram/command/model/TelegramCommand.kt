package ru.astrainteractive.messagebridge.messenger.telegram.command.model

internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand

    data class Link(val code: Int) : TelegramCommand

    sealed interface Setup : TelegramCommand

    data class Bind(val code: String) : Setup

    data object ChatInfo : Setup
}
