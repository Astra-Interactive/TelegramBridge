package ru.astrainteractive.messagebridge.messenger.telegram.connection.model

import ru.astrainteractive.messagebridge.messenger.telegram.failure.model.TelegramFailure

internal sealed interface TelegramConnectionState {
    data object Disabled : TelegramConnectionState

    data object Connecting : TelegramConnectionState

    data class Connected(val botName: String) : TelegramConnectionState

    data class Failed(val failure: TelegramFailure) : TelegramConnectionState
}
