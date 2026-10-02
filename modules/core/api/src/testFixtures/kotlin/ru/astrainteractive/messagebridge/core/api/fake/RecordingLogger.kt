package ru.astrainteractive.messagebridge.core.api.fake

import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.util.concurrent.CopyOnWriteArrayList

class RecordingLogger : Logger {
    val lines: MutableList<LogLine> = CopyOnWriteArrayList()

    override val TAG: String = "test"

    fun messagesOf(level: LogLine.Level): List<String> {
        return lines.filter { line -> line.level == level }.map(LogLine::message)
    }

    override fun error(logMessage: () -> String) {
        lines += LogLine(LogLine.Level.ERROR, logMessage.invoke())
    }

    override fun error(error: Throwable?, logMessage: () -> String) {
        lines += LogLine(LogLine.Level.ERROR, logMessage.invoke())
    }

    override fun info(logMessage: () -> String) {
        lines += LogLine(LogLine.Level.INFO, logMessage.invoke())
    }

    override fun verbose(logMessage: () -> String) {
        lines += LogLine(LogLine.Level.VERBOSE, logMessage.invoke())
    }

    override fun warn(logMessage: () -> String) {
        lines += LogLine(LogLine.Level.WARN, logMessage.invoke())
    }

    override fun debug(logMessage: () -> String) {
        lines += LogLine(LogLine.Level.DEBUG, logMessage.invoke())
    }
}
