package ru.astrainteractive.messagebridge.messenger.discord.failure.model

internal sealed interface DiscordFailure {
    val isRetryable: Boolean get() = false

    data object InvalidToken : DiscordFailure

    data class MissingIntent(val intents: List<String>) : DiscordFailure

    data object ChannelNotSet : DiscordFailure

    data class ChannelNotFound(val channelId: String) : DiscordFailure

    data class MissingPermission(val permission: String) : DiscordFailure

    data object SocksNotSupported : DiscordFailure

    data class Network(val error: String, val proxy: String?) : DiscordFailure {
        override val isRetryable: Boolean = true
    }

    data class Unknown(val error: String) : DiscordFailure {
        override val isRetryable: Boolean = true
    }
}
