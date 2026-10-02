package ru.astrainteractive.messagebridge.onboarding.impl.secret.model

internal class SecretInput(
    val words: List<String>,
    val isUnsafe: Boolean
) {
    companion object {
        private const val UNSAFE_FLAG = "--unsafe"
        private val whitespace = Regex("\\s+")

        fun parse(input: String): SecretInput {
            val words = input.trim().split(whitespace).filter(String::isNotEmpty)
            val isUnsafe = words.lastOrNull() == UNSAFE_FLAG
            return SecretInput(
                words = if (isUnsafe) words.dropLast(1) else words,
                isUnsafe = isUnsafe
            )
        }
    }
}
