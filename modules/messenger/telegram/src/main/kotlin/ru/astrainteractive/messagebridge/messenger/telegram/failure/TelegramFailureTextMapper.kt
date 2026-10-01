package ru.astrainteractive.messagebridge.messenger.telegram.failure

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.TelegramTranslation

internal class TelegramFailureTextMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

    private fun addressOf(proxy: PluginConfiguration.Proxy): String = "${proxy.host}:${proxy.port}"

    private fun mapNetwork(failure: TelegramFailure.Network, errors: TelegramTranslation.Errors): LocalizableComponent {
        return when {
            failure.proxy != null -> errors.networkProxy(addressOf(failure.proxy))
            failure.apiUrl != null -> errors.networkApiUrl(failure.apiUrl)
            else -> errors.network
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun map(failure: TelegramFailure): LocalizableComponent {
        val errors = translation.telegram.errors
        return when (failure) {
            TelegramFailure.InvalidToken -> errors.invalidToken
            TelegramFailure.ChatNotSet -> errors.chatNotSet
            TelegramFailure.ChatNotFound -> errors.chatNotFound
            is TelegramFailure.ChatMigrated -> errors.chatMigrated(failure.newChatId)
            TelegramFailure.TopicNotFound -> errors.topicNotFound
            TelegramFailure.BotNotInChat -> errors.botNotInChat
            TelegramFailure.NoRights -> errors.noRights
            TelegramFailure.TokenInUse -> errors.tokenInUse
            is TelegramFailure.RateLimited -> failure.retryAfter?.let(errors::rateLimitedFor) ?: errors.rateLimited
            is TelegramFailure.ServerError -> errors.serverError(failure.code)
            is TelegramFailure.Network -> mapNetwork(failure, errors)
            TelegramFailure.ProxyAuth -> errors.proxyAuth
            is TelegramFailure.InvalidProxy -> errors.invalidProxy(addressOf(failure.proxy))
            TelegramFailure.InvalidApiUrl -> errors.invalidApiUrl
            TelegramFailure.SocksWithPassword -> errors.socksWithPassword
            is TelegramFailure.Unknown -> errors.unknown(failure.message)
        }
    }

    /** Reads the translation when the text is shown, so a reloaded translation applies to a kept status too. */
    fun lazyMap(failure: TelegramFailure): LocalizableComponent = LocalizableComponent { locale ->
        map(failure).toComponent(locale)
    }
}
