package ru.astrainteractive.messagebridge.onboarding.impl.secret.internal

private const val VISIBLE_TOKEN_CHARS = 4
private const val MIN_MASKED_TOKEN_LENGTH = 12
private const val MASK = "…"
private val URL_CREDENTIALS = Regex("://[^/@]*@")

internal fun String.masked(): String {
    if (length < MIN_MASKED_TOKEN_LENGTH) return MASK
    return "${take(VISIBLE_TOKEN_CHARS)}$MASK${takeLast(VISIBLE_TOKEN_CHARS)}"
}

internal fun String.withoutCredentials(): String = replace(URL_CREDENTIALS, "://")
