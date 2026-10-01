package ru.astrainteractive.messagebridge.onboarding.discord.command

internal class DiscordBindCommandParser {
    fun map(contentRaw: String): String? {
        val words = contentRaw.trim().split(WHITESPACE)
        if (words.first() != BIND) return null
        return words.getOrNull(1).orEmpty()
    }

    private companion object {
        const val BIND = "!bind"
        val WHITESPACE = Regex("\\s+")
    }
}
