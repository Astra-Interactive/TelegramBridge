package ru.astrainteractive.messagebridge.messenger.telegram.request

import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure

internal sealed interface TelegramRequestResult<out T> {
    data class Success<T>(val value: T) : TelegramRequestResult<T>

    /** No bot is running: the token is empty or the settings cannot work, which the status already tells. */
    data object NotConnected : TelegramRequestResult<Nothing>

    data class Failed(val failure: TelegramFailure) : TelegramRequestResult<Nothing>
}
