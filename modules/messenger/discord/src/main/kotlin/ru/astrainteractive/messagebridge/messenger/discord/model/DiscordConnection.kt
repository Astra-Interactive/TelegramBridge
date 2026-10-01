package ru.astrainteractive.messagebridge.messenger.discord.model

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.JDA
import kotlin.time.Duration

internal sealed interface DiscordConnection {
    data object Disabled : DiscordConnection

    data object Connecting : DiscordConnection

    data class Connected(val jda: JDA) : DiscordConnection

    /** Stays while the next attempt is made, when [DiscordFailure.isRetryable]. */
    data class Failed(val failure: DiscordFailure) : DiscordConnection
}

/** @return the state after the bot stops connecting, or [Connecting][DiscordConnection.Connecting] after [timeout] */
internal suspend fun StateFlow<DiscordConnection>.awaitSettled(timeout: Duration): DiscordConnection {
    return withTimeoutOrNull(timeout) { first { connection -> connection !is DiscordConnection.Connecting } } ?: value
}

internal suspend fun StateFlow<DiscordConnection>.awaitJda(timeout: Duration): JDA? {
    return (awaitSettled(timeout) as? DiscordConnection.Connected)?.jda
}
