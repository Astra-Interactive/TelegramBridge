package ru.astrainteractive.messagebridge.messenger.telegram.message.model

internal sealed interface TelegramCommand {
    data object Vanilla : TelegramCommand
}
