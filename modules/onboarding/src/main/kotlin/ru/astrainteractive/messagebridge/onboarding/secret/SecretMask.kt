package ru.astrainteractive.messagebridge.onboarding.secret

private const val VISIBLE_TOKEN_CHARS = 4
private const val MIN_MASKED_TOKEN_LENGTH = 12
private const val MASK = "…"
private val URL_CREDENTIALS = Regex("://[^/@]*@")

/** The first and the last characters of [token]: enough to tell two tokens apart, not enough to use one. */
internal fun maskToken(token: String): String {
    if (token.length < MIN_MASKED_TOKEN_LENGTH) return MASK
    return "${token.take(VISIBLE_TOKEN_CHARS)}$MASK${token.takeLast(VISIBLE_TOKEN_CHARS)}"
}

/** [url] without the `user:password@` part a proxy or a mirror may take. */
internal fun withoutCredentials(url: String): String = url.replace(URL_CREDENTIALS, "://")
