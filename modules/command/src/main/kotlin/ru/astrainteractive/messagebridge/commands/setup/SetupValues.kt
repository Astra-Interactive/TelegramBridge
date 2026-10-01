package ru.astrainteractive.messagebridge.commands.setup

import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KPlayerKCommandSender
import ru.astrainteractive.astralibs.command.api.exception.LocalizableComponentCommandException
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import java.net.URI

private val HTTP_SCHEMES = setOf("http", "https")

/** The Telegram module takes a server address only: a path or a query would break the Bot API methods. */
private fun URI.isServerAddress(): Boolean {
    val hasHttpScheme = scheme?.lowercase() in HTTP_SCHEMES
    val hasNoPath = rawPath.isNullOrEmpty() || rawPath == "/"
    return hasHttpScheme && !host.isNullOrBlank() && hasNoPath && rawQuery == null && rawFragment == null
}

/** Reads the values of the /mb commands and fails with a text that tells what a valid value looks like. */
internal class SetupValues(translationKrate: CachedKrate<PluginTranslation>) {
    private val translation by translationKrate

    private fun fail(text: LocalizableComponent): Nothing = throw LocalizableComponentCommandException(text)

    /** Paper writes the commands of players into latest.log, so a player has to confirm a secret. */
    fun requireSecretAllowed(sender: KCommandSender, input: SecretInput) {
        if (sender is KPlayerKCommandSender && !input.isUnsafe) fail(translation.setup.unsafeRequired)
    }

    fun telegramToken(input: SecretInput): String {
        return input.words.singleOrNull()
            ?.takeIf(TELEGRAM_TOKEN::matches)
            ?: fail(translation.setup.invalidTelegramToken)
    }

    fun discordToken(input: SecretInput): String {
        return input.words.singleOrNull()
            ?.takeIf(DISCORD_TOKEN::matches)
            ?: fail(translation.setup.invalidDiscordToken)
    }

    fun chatId(value: String): String {
        return value.toLongOrNull()
            ?.takeIf { chatId -> chatId != 0L }
            ?.toString()
            ?: fail(translation.setup.invalidChat)
    }

    /** @return the topic, or an empty string for [NONE] */
    fun topicId(value: String): String {
        if (value.equals(NONE, ignoreCase = true)) return ""
        return value.toIntOrNull()
            ?.takeIf { topicId -> topicId > 0 }
            ?.toString()
            ?: fail(translation.setup.invalidTopic)
    }

    fun channelId(value: String): String {
        return value.takeIf(DISCORD_SNOWFLAKE::matches) ?: fail(translation.setup.invalidChannel)
    }

    /** @return the address with its scheme, https when none is typed, or an empty string for [DEFAULT] */
    fun apiUrl(value: String): String {
        if (value.equals(DEFAULT, ignoreCase = true)) return ""
        val url = if ("://" in value) value else "https://$value"
        val isServerAddress = runCatching { URI(url) }.getOrNull()?.isServerAddress() == true
        if (!isServerAddress) fail(translation.setup.invalidUrl)
        return url.trimEnd('/')
    }

    fun proxyType(value: String): PluginConfiguration.Proxy.Type {
        return when (value.lowercase()) {
            HTTP -> PluginConfiguration.Proxy.Type.HTTP
            SOCKS5, SOCKS -> PluginConfiguration.Proxy.Type.SOCKS5
            else -> fail(translation.setup.invalidProxyType)
        }
    }

    /** The websocket client JDA reaches the Discord gateway with takes an HTTP proxy only. */
    fun discordProxyType(value: String): PluginConfiguration.Proxy.Type {
        val type = proxyType(value)
        if (type != PluginConfiguration.Proxy.Type.HTTP) fail(translation.setup.socksNotSupported)
        return type
    }

    /** @param credentials the username and the password, the password being a secret */
    fun proxy(
        sender: KCommandSender,
        type: PluginConfiguration.Proxy.Type,
        host: String,
        port: String,
        credentials: SecretInput
    ): PluginConfiguration.Proxy {
        val words = credentials.words
        if (words.size > MAX_CREDENTIALS) fail(translation.setup.invalidProxyCredentials)
        val password = words.getOrNull(1)
        if (password != null) requireSecretAllowed(sender, credentials)
        return PluginConfiguration.Proxy(
            type = type,
            host = host.takeIf(PROXY_HOST::matches) ?: fail(translation.setup.invalidHost),
            port = port.toIntOrNull()?.takeIf { number -> number in MIN_PORT..MAX_PORT }
                ?: fail(translation.setup.invalidPort),
            username = words.getOrNull(0),
            password = password
        )
    }

    companion object {
        const val NONE = "none"
        const val DEFAULT = "default"
        const val HTTP = "http"
        const val SOCKS5 = "socks5"
        private const val SOCKS = "socks"
        private const val MIN_PORT = 1
        private const val MAX_PORT = 65535
        private const val MAX_CREDENTIALS = 2
        private val TELEGRAM_TOKEN = Regex("^\\d+:[A-Za-z0-9_-]{30,}$")
        private val DISCORD_TOKEN = Regex("^[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{10,}$")
        private val DISCORD_SNOWFLAKE = Regex("^\\d{17,20}$")
        private val PROXY_HOST = Regex("^[A-Za-z0-9.-]+$|^[0-9A-Fa-f:]+$")
    }
}
