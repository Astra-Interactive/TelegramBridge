package ru.astrainteractive.messagebridge.onboarding.telegram.model

internal sealed interface TelegramSetupCommand {
    data class Bind(val code: String) : TelegramSetupCommand

    data object ChatInfo : TelegramSetupCommand
}
