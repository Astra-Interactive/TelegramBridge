package ru.astrainteractive.messagebridge.messenger.discord.api.model

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration

sealed interface DiscordConnection {
    data object Disabled : DiscordConnection

    data object Connecting : DiscordConnection

    data class Connected(val jda: JDA) : DiscordConnection

    data class Failed(val failure: DiscordFailure) : DiscordConnection
}

suspend fun StateFlow<DiscordConnection>.awaitSettled(timeout: Duration): DiscordConnection {
    return withTimeoutOrNull(timeout) { first { connection -> connection !is DiscordConnection.Connecting } } ?: value
}

suspend fun StateFlow<DiscordConnection>.awaitJda(timeout: Duration): JDA? {
    return (awaitSettled(timeout) as? DiscordConnection.Connected)?.jda
}
