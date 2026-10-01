package ru.astrainteractive.messagebridge.messenger.discord.impl.connection.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.dv8tion.jda.api.JDA
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.impl.connection.model.DiscordConnectionSettings
import ru.astrainteractive.messagebridge.messenger.discord.impl.connection.network.DiscordConnector
import ru.astrainteractive.messagebridge.messenger.discord.impl.failure.mapping.DiscordFailureMapper

internal class DiscordSession(
    connector: DiscordConnector,
    private val failureMapper: DiscordFailureMapper,
    configFlow: StateFlow<PluginConfiguration>,
    translationKrate: CachedKrate<PluginTranslation>,
    scope: CoroutineScope,
) : Logger by JUtiltLogger("MessageBridge-DiscordSession") {
    private val translation by translationKrate

    private val reconnectRequests = MutableStateFlow(0)

    val connection: StateFlow<DiscordConnection> = combine(
        flow = configFlow
            .map { config ->
                DiscordConnectionSettings(
                    jdaConfig = config.jdaConfig.copy(channelId = ""),
                    intents = DiscordIntents.requiredBy(config)
                )
            }
            .distinctUntilChanged(),
        flow2 = reconnectRequests,
        transform = { settings, _ -> settings }
    )
        .flatMapLatest(connector::connect)
        .distinctUntilChanged()
        .onEach(::logConnection)
        .stateIn(scope, SharingStarted.Eagerly, DiscordConnection.Connecting)

    val jda: Flow<JDA?> = connection
        .map { connection -> (connection as? DiscordConnection.Connected)?.jda }
        .distinctUntilChanged()

    private fun logConnection(connection: DiscordConnection) {
        val console = translation.discord.console
        when (connection) {
            DiscordConnection.Disabled -> {
                info { console.disabled.toMessengerText() }
            }

            DiscordConnection.Connecting -> verbose { "#logConnection connecting to Discord" }
            is DiscordConnection.Connected -> info { console.connected(connection.jda.selfUser.name).toMessengerText() }
            is DiscordConnection.Failed -> error {
                val reason = failureMapper.toText(connection.failure, translation.discord)
                console.notConnected(reason).toMessengerText()
            }
        }
    }

    fun reconnectIfFailed() {
        if (connection.value is DiscordConnection.Failed) reconnectRequests.update { requests -> requests + 1 }
    }
}
