package ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.impl.secret.internal.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.impl.secret.model.SecretInput
import ru.astrainteractive.messagebridge.onboarding.impl.util.refuse
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

    fun token(sender: KCommandSender, value: String): Result<String> {
        val input = secretGuard.allow(sender, SecretInput.parse(value))
            .getOrElse { t -> return Result.failure(t) }
        val token = input.words.singleOrNull()?.takeIf(TOKEN::matches)
            ?: return refuse(translation.setup.invalidTelegramToken)
        return Result.success(token)
    }

    fun chat(value: String): Result<String> {
        val chatId = value.toLongOrNull()
            ?.takeIf { chatId -> chatId != 0L }
            ?.toString()
            ?: return refuse(translation.setup.invalidChat)
        return Result.success(chatId)
    }

    fun topic(value: String): Result<String> {
        if (value.equals(NONE, ignoreCase = true)) return Result.success("")
        val topicId = value.toIntOrNull()
            ?.takeIf { topicId -> topicId > 0 }
            ?.toString()
            ?: return refuse(translation.setup.invalidTopic)
        return Result.success(topicId)
    }

    fun apiUrl(value: String): Result<String> {
        val typed = value.trim()
        val url = when {
            typed.equals(DEFAULT, ignoreCase = true) -> ""
            "://" in typed -> typed
            else -> "$DEFAULT_SCHEME$typed"
        }
        if (url.isNotEmpty() && !url.isServerAddress()) return refuse(translation.setup.invalidUrl)
        return Result.success(url.trimEnd('/'))
    }

    companion object {
        const val NONE = "none"
        const val DEFAULT = "default"
        private const val DEFAULT_SCHEME = "https://"
        private val TOKEN = Regex("^\\d+:[A-Za-z0-9_-]{30,}$")
    }
}
