package ru.astrainteractive.messagebridge.messenger.telegram.fake

internal data class LogLine(
    val level: Level,
    val message: String
) {
    enum class Level { ERROR, WARN, INFO, DEBUG, VERBOSE }
}
