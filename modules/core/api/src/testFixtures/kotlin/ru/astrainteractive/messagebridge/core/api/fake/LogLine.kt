package ru.astrainteractive.messagebridge.core.api.fake

data class LogLine(
    val level: Level,
    val message: String
) {
    enum class Level { ERROR, WARN, INFO, DEBUG, VERBOSE }
}
