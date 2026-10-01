package ru.astrainteractive.messagebridge.messenger.telegram.connection.api

import ru.astrainteractive.messagebridge.messenger.telegram.connection.model.TelegramConnectionState

internal interface TelegramBotSession {
    suspend fun fetchBotUserName(): Result<String>

    suspend fun startPolling(botName: String, onState: (TelegramConnectionState) -> Unit): Result<AutoCloseable>
}
