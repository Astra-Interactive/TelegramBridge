package ru.astrainteractive.messagebridge.onboarding.secret.internal

private const val VISIBLE_TOKEN_CHARS = 4
private const val MIN_MASKED_TOKEN_LENGTH = 12
private const val MASK = "…"
private val URL_CREDENTIALS = Regex("://[^/@]*@")

internal fun maskToken(token: String): String {
    if (token.length < MIN_MASKED_TOKEN_LENGTH) return MASK
    return "${token.take(VISIBLE_TOKEN_CHARS)}$MASK${token.takeLast(VISIBLE_TOKEN_CHARS)}"
}

internal fun withoutCredentials(url: String): String = url.replace(URL_CREDENTIALS, "://")
