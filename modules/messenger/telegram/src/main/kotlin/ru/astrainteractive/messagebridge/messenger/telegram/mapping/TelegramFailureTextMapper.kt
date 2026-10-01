package ru.astrainteractive.messagebridge.messenger.telegram.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.telegram.model.TelegramFailure

internal class TelegramFailureTextMapper(
    translationKrate: CachedKrate<PluginTranslation>,
) {
    private val translation by translationKrate

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
            is TelegramFailure.Network -> mapNetwork(failure)
            TelegramFailure.ProxyAuth -> errors.proxyAuth
            TelegramFailure.InvalidApiUrl -> errors.invalidApiUrl
            TelegramFailure.SocksWithPassword -> errors.socksWithPassword
            is TelegramFailure.Unknown -> errors.unknown(failure.message)
        }
    }

    private fun mapNetwork(failure: TelegramFailure.Network): LocalizableComponent {
        val errors = translation.telegram.errors
        return when {
            failure.proxy != null -> errors.networkProxy("${failure.proxy.host}:${failure.proxy.port}")
            failure.apiUrl != null -> errors.networkApiUrl(failure.apiUrl)
            else -> errors.network
        }
    }

    /** Reads the translation when the text is shown, so a reloaded translation applies to a kept status too. */
    fun lazyMap(failure: TelegramFailure): LocalizableComponent = LocalizableComponent { locale ->
        map(failure).toComponent(locale)
    }
}
