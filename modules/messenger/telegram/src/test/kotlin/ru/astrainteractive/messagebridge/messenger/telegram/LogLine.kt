package ru.astrainteractive.messagebridge.messenger.telegram

internal data class LogLine(
    val level: Level,
    val message: String
) {
    enum class Level { ERROR, WARN, INFO, DEBUG, VERBOSE }
}
