package ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration.TelegramConfig
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.impl.model.refuse
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.masked
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.withoutCredentials
import ru.astrainteractive.messagebridge.onboarding.impl.secret.model.SecretInput
import ru.astrainteractive.messagebridge.onboarding.impl.setting.model.Setting
import java.net.URI

private val HTTP_SCHEMES = setOf("http", "https")

private fun URI.isServerAddress(): Boolean {
    val hasHttpScheme = scheme?.lowercase() in HTTP_SCHEMES
    val hasNoPath = rawPath.isNullOrEmpty() || rawPath == "/"
    return hasHttpScheme && !host.isNullOrBlank() && hasNoPath && rawQuery == null && rawFragment == null
}

private fun String.isServerAddress(): Boolean {
    return runCatching { URI(this) }.fold(
        onSuccess = { uri -> uri.isServerAddress() },
        onFailure = { _ -> false }
    )
}

internal class TelegramSettings(
    private val secretGuard: SecretGuard,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    fun token(sender: KCommandSender, value: String): Result<Setting<TelegramConfig>> {
        val input = secretGuard.allow(sender, SecretInput.parse(value))
            .getOrElse { error -> return Result.failure(error) }
        val token = input.words.singleOrNull()?.takeIf(TOKEN::matches)
            ?: return refuse(translation.setup.invalidTelegramToken)
        val setting = Setting<TelegramConfig>(saved = translation.setup.saved.token(token.masked())) { tgConfig ->
            tgConfig.copy(token = token)
        }
        return Result.success(setting)
    }

    fun chat(value: String): Result<Setting<TelegramConfig>> {
        val chatId = value.toLongOrNull()
            ?.takeIf { chatId -> chatId != 0L }
            ?.toString()
            ?: return refuse(translation.setup.invalidChat)
        val setting = Setting<TelegramConfig>(saved = translation.setup.saved.chat(chatId)) { tgConfig ->
            tgConfig.copy(chatID = chatId)
        }
        return Result.success(setting)
    }

    fun topic(value: String): Result<Setting<TelegramConfig>> {
        val topicId = when {
            value.equals(NONE, ignoreCase = true) -> ""
            else -> value.toIntOrNull()
                ?.takeIf { topicId -> topicId > 0 }
                ?.toString()
                ?: return refuse(translation.setup.invalidTopic)
        }
        val saved = if (topicId.isEmpty()) {
            translation.setup.saved.topicRemoved
        } else {
            translation.setup.saved.topic(topicId)
        }
        return Result.success(Setting<TelegramConfig>(saved = saved) { tgConfig -> tgConfig.copy(topicID = topicId) })
    }

    fun apiUrl(value: String): Result<Setting<TelegramConfig>> {
        val typed = value.trim()
        val url = when {
            typed.equals(DEFAULT, ignoreCase = true) -> ""
            "://" in typed -> typed
            else -> "$DEFAULT_SCHEME$typed"
        }
        if (url.isNotEmpty() && !url.isServerAddress()) return refuse(translation.setup.invalidUrl)
        val address = url.trimEnd('/')
        val saved = if (address.isEmpty()) {
            translation.setup.saved.apiUrlRemoved
        } else {
            translation.setup.saved.apiUrl(address.withoutCredentials())
        }
        return Result.success(Setting<TelegramConfig>(saved = saved) { tgConfig -> tgConfig.copy(apiUrl = address) })
    }

    companion object {
        const val NONE = "none"
        const val DEFAULT = "default"
        private const val DEFAULT_SCHEME = "https://"
        private val TOKEN = Regex("^\\d+:[A-Za-z0-9_-]{30,}$")
    }
}
