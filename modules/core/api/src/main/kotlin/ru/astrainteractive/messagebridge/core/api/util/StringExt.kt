package ru.astrainteractive.messagebridge.core.api.util

fun String.ellipsize(maxLength: Int): String {
    if (length <= maxLength) return this
    val end = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
    return substring(0, end).trimEnd() + "…"
}
