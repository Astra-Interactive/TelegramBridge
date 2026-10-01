package ru.astrainteractive.messagebridge.commands.setup

import ru.astrainteractive.messagebridge.core.PluginConfiguration

private const val VISIBLE_TOKEN_CHARS = 4
private const val MIN_MASKED_TOKEN_LENGTH = 12
private val URL_CREDENTIALS = Regex("://[^/@]*@")

/** The first and the last characters of [token]: enough to tell two tokens apart, not enough to use one. */
internal fun maskToken(token: String): String {
    if (token.length < MIN_MASKED_TOKEN_LENGTH) return "…"
    return "${token.take(VISIBLE_TOKEN_CHARS)}…${token.takeLast(VISIBLE_TOKEN_CHARS)}"
}

/** The proxy without its username and password, e.g. `HTTP 1.2.3.4:8080`. */
internal fun PluginConfiguration.Proxy.describe(): String = "$type $host:$port"

internal fun withoutCredentials(url: String): String = url.replace(URL_CREDENTIALS, "://")
