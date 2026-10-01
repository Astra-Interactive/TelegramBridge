package ru.astrainteractive.messagebridge.messenger.telegram.api.model

data class TelegramBotCommand(
    val name: String,
    val argument: String
) {
    companion object {
        private const val MENTION = '@'
        private val WHITESPACE = "\\s+".toRegex()

        private fun String.isFor(botUserName: String?): Boolean {
            val mention = substringAfter(MENTION, missingDelimiterValue = "")
            if (mention.isEmpty() || botUserName == null) return true
            return mention.equals(botUserName, ignoreCase = true)
        }

        fun parse(text: String, botUserName: String?): TelegramBotCommand? {
            val parts = text.trim().split(WHITESPACE, limit = 2)
            val command = parts.first()
            if (!command.isFor(botUserName)) return null
            return TelegramBotCommand(
                name = command.substringBefore(MENTION),
                argument = parts.getOrNull(1).orEmpty().trim()
            )
        }
    }
}
