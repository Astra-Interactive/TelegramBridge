package ru.astrainteractive.messagebridge.onboarding.telegram

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration.TelegramConfig
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.input.refuse
import ru.astrainteractive.messagebridge.onboarding.secret.SecretGuard
import ru.astrainteractive.messagebridge.onboarding.secret.SecretInput
import ru.astrainteractive.messagebridge.onboarding.secret.maskToken
import ru.astrainteractive.messagebridge.onboarding.secret.withoutCredentials
import ru.astrainteractive.messagebridge.onboarding.setting.Setting
import java.net.URI

private val HTTP_SCHEMES = setOf("http", "https")

/** The Telegram module takes a server address only: a path or a query would break the Bot API methods. */
private fun URI.isServerAddress(): Boolean {
    val hasHttpScheme = scheme?.lowercase() in HTTP_SCHEMES
    val hasNoPath = rawPath.isNullOrEmpty() || rawPath == "/"
    return hasHttpScheme && !host.isNullOrBlank() && hasNoPath && rawQuery == null && rawFragment == null
}

private fun isServerAddress(url: String): Boolean {
    return runCatching { URI(url) }.fold(
        onSuccess = { uri -> uri.isServerAddress() },
        onFailure = { _ -> false }
    )
}

/** Reads the values typed after /mb telegram into the settings they save. */
internal class TelegramSettings(
    private val secretGuard: SecretGuard,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    fun token(sender: KCommandSender, value: String): Result<Setting<TelegramConfig>> {
        val input = secretGuard.allow(sender, SecretInput.parse(value))
            .getOrElse { error -> return Result.failure(error) }
        val token = input.words.singleOrNull()?.takeIf(TOKEN::matches)
            ?: return refuse(translation.setup.invalidTelegramToken)
        val setting = Setting<TelegramConfig>(saved = translation.setup.saved.token(maskToken(token))) { tgConfig ->
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

    /** [NONE] removes the topic, so messages go to the whole chat. */
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

    /** Takes https when no scheme is typed; [DEFAULT] goes back to api.telegram.org. */
    fun apiUrl(value: String): Result<Setting<TelegramConfig>> {
        val typed = value.trim()
        val url = when {
            typed.equals(DEFAULT, ignoreCase = true) -> ""
            "://" in typed -> typed
            else -> "$DEFAULT_SCHEME$typed"
        }
        if (url.isNotEmpty() && !isServerAddress(url)) return refuse(translation.setup.invalidUrl)
        val address = url.trimEnd('/')
        val saved = if (address.isEmpty()) {
            translation.setup.saved.apiUrlRemoved
        } else {
            translation.setup.saved.apiUrl(withoutCredentials(address))
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
