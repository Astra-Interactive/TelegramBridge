package ru.astrainteractive.messagebridge.messenger.discord.model

/** Why the bot could not connect or deliver a message, in terms an admin can act on. */
internal sealed interface DiscordFailure {
    /** Whether trying again can help; the other failures last until the settings change. */
    val isRetryable: Boolean get() = false

    data object InvalidToken : DiscordFailure

    data object MissingIntent : DiscordFailure

    data object ChannelNotSet : DiscordFailure

    data class ChannelNotFound(val channelId: String) : DiscordFailure

    /** @param permission name of the permission as Discord shows it, e.g. `Manage Webhooks` */
    data class MissingPermission(val permission: String) : DiscordFailure

    data object SocksNotSupported : DiscordFailure

    /** @param proxy `host:port` of the proxy the request went through, `null` without a proxy */
    data class Network(val error: String, val proxy: String?) : DiscordFailure {
        override val isRetryable: Boolean = true
    }

    data class Unknown(val error: String) : DiscordFailure {
        override val isRetryable: Boolean = true
    }
}

/** A [DiscordFailure] found by the bridge itself, before Discord could report it. */
internal class DiscordFailureException(val failure: DiscordFailure) : RuntimeException("$failure")

/** JDA stopped by itself; [code] is the close code of the websocket, `null` when it did not report one. */
internal class JdaShutdownException(val code: Int?, cause: Throwable? = null) :
    RuntimeException("JDA stopped with close code $code", cause)
