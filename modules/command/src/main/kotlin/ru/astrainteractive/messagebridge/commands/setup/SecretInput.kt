package ru.astrainteractive.messagebridge.commands.setup

/** Words typed after a subcommand that may hold a secret, with the [UNSAFE_FLAG] taken off the end. */
internal class SecretInput(
    val words: List<String>,
    val isUnsafe: Boolean
) {
    companion object {
        const val UNSAFE_FLAG = "--unsafe"
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
