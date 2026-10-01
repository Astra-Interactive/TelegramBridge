package ru.astrainteractive.messagebridge.messenger.telegram.failure

import ru.astrainteractive.messagebridge.core.PluginConfiguration
import kotlin.time.Duration

/** Why a request to Telegram failed, in terms an admin can act on. */
internal sealed interface TelegramFailure {
    /** The failure passes by itself, so the same request is worth repeating. */
    val isTransient: Boolean get() = false

    /** Only another setting fixes the failure, so connecting again with the same one is pointless. */
    val needsNewSettings: Boolean get() = false

    data object InvalidToken : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data object ChatNotSet : TelegramFailure

    data object ChatNotFound : TelegramFailure

    /** A group became a supergroup, which has another id. */
    data class ChatMigrated(val newChatId: Long) : TelegramFailure

    data object TopicNotFound : TelegramFailure

    data object BotNotInChat : TelegramFailure

    data object NoRights : TelegramFailure

    /** Another program polls updates with the same token. */
    data object TokenInUse : TelegramFailure

    /** @param retryAfter how long to wait before the next request, when Telegram tells it */
    data class RateLimited(val retryAfter: Duration?) : TelegramFailure {
        override val isTransient: Boolean = true
    }

    data class ServerError(val code: Int) : TelegramFailure {
        override val isTransient: Boolean = true
    }

    /**
     * @param proxy the proxy the request went through
     * @param apiUrl the Bot API server the request went to, `null` for the Telegram one
     */
    data class Network(
        val proxy: PluginConfiguration.Proxy?,
        val apiUrl: String?
    ) : TelegramFailure {
        override val isTransient: Boolean = true
    }

    data object ProxyAuth : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    /** The proxy has no host or a port out of range, so no connection can be opened through it. */
    data class InvalidProxy(val proxy: PluginConfiguration.Proxy) : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data object InvalidApiUrl : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    /** The JDK authenticates to a SOCKS proxy only through a global `Authenticator`, which is not installed. */
    data object SocksWithPassword : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data class Unknown(val message: String) : TelegramFailure
}
