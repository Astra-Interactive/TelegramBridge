package ru.astrainteractive.messagebridge.messenger.telegram.api.model

sealed interface TelegramConnectionState {
    data object Disabled : TelegramConnectionState

    data object Connecting : TelegramConnectionState

    data class Connected(val botName: String) : TelegramConnectionState

    data class Failed(val failure: TelegramFailure) : TelegramConnectionState
}

val TelegramConnectionState.botUserName: String?
    get() = when (this) {
        is TelegramConnectionState.Connected -> botName.removePrefix("@")
        else -> null
    }
