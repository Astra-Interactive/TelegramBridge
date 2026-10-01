package ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.config.TelegramTranslation
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailureTextMapper

internal class TelegramFailureTextMapperImpl(
    translationKrate: CachedKrate<PluginTranslation>,
) : TelegramFailureTextMapper {
    private val translation by translationKrate

    private fun addressOf(proxy: PluginConfiguration.Proxy): String = "${proxy.host}:${proxy.port}"

    private fun mapNetwork(failure: TelegramFailure.Network, errors: TelegramTranslation.Errors): LocalizableComponent {
        val proxy = failure.proxy
        val apiUrl = failure.apiUrl
        return when {
            proxy != null -> errors.networkProxy(addressOf(proxy))
            apiUrl != null -> errors.networkApiUrl(apiUrl)
            else -> errors.network
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun map(failure: TelegramFailure): LocalizableComponent {
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

    override fun lazyMap(failure: TelegramFailure): LocalizableComponent = LocalizableComponent { locale ->
        map(failure).toComponent(locale)
    }
}
