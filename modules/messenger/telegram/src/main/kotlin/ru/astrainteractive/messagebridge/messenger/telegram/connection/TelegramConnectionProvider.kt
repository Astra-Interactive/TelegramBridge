package ru.astrainteractive.messagebridge.messenger.telegram.connection

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import ru.astrainteractive.messagebridge.core.PluginConfiguration

/**
 * Opens a connection whenever a setting it depends on changes, and closes the connection it replaces; the chat
 * and the limits change without reconnecting.
 */
internal class TelegramConnectionProvider(
    private val configFlow: StateFlow<PluginConfiguration>,
    private val connectionFactory: TelegramConnectionFactory,
) {
    val connections: Flow<TelegramConnection> = configFlow
        .map { configuration -> TelegramConnectionSettings.of(configuration.tgConfig) }
        .distinctUntilChanged()
        .flatMapLatest { settings -> connectionOf(settings) }

    private fun connectionOf(settings: TelegramConnectionSettings): Flow<TelegramConnection> = callbackFlow {
        val connection = connectionFactory.create(settings)
        send(connection)
        awaitClose { (connection as? TelegramConnection.Ready)?.close() }
    }
}
