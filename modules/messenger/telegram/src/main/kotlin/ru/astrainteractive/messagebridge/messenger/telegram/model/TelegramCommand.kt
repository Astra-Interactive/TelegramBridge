package ru.astrainteractive.messagebridge.messenger.telegram.model

internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand
}
