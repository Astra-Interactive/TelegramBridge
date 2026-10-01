package ru.astrainteractive.messagebridge.core.config

import com.charleskorn.kaml.YamlException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import ru.astrainteractive.astralibs.util.parse
import ru.astrainteractive.astralibs.util.writeIntoFile
import ru.astrainteractive.klibs.kstorage.api.StateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.asStateFlowMutableKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import java.io.File

/**
 * A YAML file that keeps the last value it could read. A file with an error is left untouched and not applied,
 * so a typo in a token does not replace the whole configuration with defaults.
 */
class YamlConfigFile<T>(
    private val stringFormat: StringFormat,
    private val serializer: KSerializer<T>,
    val file: File,
    private val factory: () -> T
) : Logger by JUtiltLogger("MessageBridge-${file.name}") {
    private var lastValue: T? = null

    /** The error of the last read, `null` when the file was read fine. */
    @Volatile
    var lastError: String? = null
        private set

    fun parse(): Result<T> = stringFormat.parse(serializer, file)

    /** @return the error the file has now, `null` when it can be read or does not exist yet */
    fun check(): String? {
        if (!file.exists() || file.length() == 0L) return null
        return parse().exceptionOrNull()?.let(::describe)
    }

    fun load(): T {
        if (!file.exists() || file.length() == 0L) {
            return factory.invoke().also(::save)
        }
        return parse()
            .onSuccess { value ->
                lastValue = value
                lastError = null
                save(value)
            }
            .getOrElse { throwable ->
                val reason = describe(throwable)
                lastError = reason
                error { "${file.name} has an error and is not applied, the previous settings are kept: $reason" }
                lastValue ?: factory.invoke()
            }
    }

    fun save(value: T) {
        stringFormat.writeIntoFile(serializer, value, file)
        lastValue = value
    }

    fun krate(): StateFlowMutableKrate<T> = DefaultMutableKrate(
        factory = factory,
        saver = ::save,
        loader = ::load
    ).asStateFlowMutableKrate()

    private fun describe(throwable: Throwable): String = when (throwable) {
        is YamlException -> "line ${throwable.line}, column ${throwable.column}: ${throwable.message}"
        else -> throwable.message ?: throwable::class.java.simpleName
    }
}
