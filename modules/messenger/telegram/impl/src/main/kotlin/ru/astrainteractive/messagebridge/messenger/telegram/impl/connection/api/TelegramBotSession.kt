package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.api

import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState

internal interface TelegramBotSession {
    suspend fun fetchBotUserName(): Result<String>

    suspend fun startPolling(botName: String, onState: (TelegramConnectionState) -> Unit): Result<AutoCloseable>
}
