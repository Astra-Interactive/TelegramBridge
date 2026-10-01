package ru.astrainteractive.messagebridge.messenger.telegram.connection

/**
 * The bot on one [TelegramConnection.Ready]. A failure is what the Telegram library threw; [TelegramBotConnector]
 * tells it as a [ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailure].
 */
internal interface TelegramBotSession {
    /** @return username of the bot without `@`, which also proves the token works */
    suspend fun fetchBotUserName(): Result<String>

    /**
     * Starts polling updates in the background; closing the result stops it.
     *
     * @param onState receives the state of the polling from the answers of Telegram, until the polling is stopped
     */
    suspend fun startPolling(botName: String, onState: (TelegramConnectionState) -> Unit): Result<AutoCloseable>
}
