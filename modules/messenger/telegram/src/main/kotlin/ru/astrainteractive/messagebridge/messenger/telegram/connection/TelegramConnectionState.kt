package ru.astrainteractive.messagebridge.messenger.telegram.connection

import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure

/** State of the bot connection; unlike its text, a [TelegramFailure] compares by value, so repeats are dropped. */
internal sealed interface TelegramConnectionState {
    data object Disabled : TelegramConnectionState

    data object Connecting : TelegramConnectionState

    data class Connected(val botName: String) : TelegramConnectionState

    data class Failed(val failure: TelegramFailure) : TelegramConnectionState
}
