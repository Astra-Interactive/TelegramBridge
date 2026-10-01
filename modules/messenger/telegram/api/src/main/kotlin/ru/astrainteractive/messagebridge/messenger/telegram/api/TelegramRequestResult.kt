package ru.astrainteractive.messagebridge.messenger.telegram.api

sealed interface TelegramRequestResult<out T> {
    data class Success<T>(val value: T) : TelegramRequestResult<T>

    data object NotConnected : TelegramRequestResult<Nothing>

    data class Failed(val failure: TelegramFailure) : TelegramRequestResult<Nothing>
}
