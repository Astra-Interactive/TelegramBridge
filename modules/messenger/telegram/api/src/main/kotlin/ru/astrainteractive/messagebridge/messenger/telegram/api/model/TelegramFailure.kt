package ru.astrainteractive.messagebridge.messenger.telegram.api.model

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import kotlin.time.Duration

sealed interface TelegramFailure {
    val isTransient: Boolean get() = false

    val needsNewSettings: Boolean get() = false

    data object InvalidToken : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data object ChatNotSet : TelegramFailure

    data object ChatNotFound : TelegramFailure

    data class ChatMigrated(val newChatId: Long) : TelegramFailure

    data object TopicNotFound : TelegramFailure

    data object BotNotInChat : TelegramFailure

    data object NoRights : TelegramFailure

    data object TokenInUse : TelegramFailure

    data class RateLimited(val retryAfter: Duration?) : TelegramFailure {
        override val isTransient: Boolean = true
    }

    data class ServerError(val code: Int) : TelegramFailure {
        override val isTransient: Boolean = true
    }

    data class Network(
        val proxy: PluginConfiguration.Proxy?,
        val apiUrl: String?
    ) : TelegramFailure {
        override val isTransient: Boolean = true
    }

    data object ProxyAuth : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data class InvalidProxy(val proxy: PluginConfiguration.Proxy) : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data object InvalidApiUrl : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data object SocksWithPassword : TelegramFailure {
        override val needsNewSettings: Boolean = true
    }

    data class Unknown(val message: String) : TelegramFailure
}
