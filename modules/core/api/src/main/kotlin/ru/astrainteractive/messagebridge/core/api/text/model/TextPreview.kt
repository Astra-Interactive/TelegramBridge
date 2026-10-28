@file:Suppress("CompanionFactoryFunction")

package ru.astrainteractive.messagebridge.core.api.text.model

@JvmInline
value class TextPreview private constructor(val value: String) {
    companion object {
        private const val ELLIPSIS = "…"

        fun of(text: String, maxLength: Int): TextPreview {
            if (text.length <= maxLength) return TextPreview(text)
            val end = if (text[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
            return TextPreview(text.substring(0, end).trimEnd() + ELLIPSIS)
        }
    }
}
