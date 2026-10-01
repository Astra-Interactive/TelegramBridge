package ru.astrainteractive.messagebridge.messenger.telegram.api

sealed interface TelegramConnectionState {
    data object Disabled : TelegramConnectionState

    data object Connecting : TelegramConnectionState

    data class Connected(val botName: String) : TelegramConnectionState

    data class Failed(val failure: TelegramFailure) : TelegramConnectionState
}
