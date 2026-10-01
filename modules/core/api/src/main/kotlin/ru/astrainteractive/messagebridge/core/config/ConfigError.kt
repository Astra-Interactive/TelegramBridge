package ru.astrainteractive.messagebridge.core.config

import com.charleskorn.kaml.YamlException

/** @return what is wrong with a YAML file, with the line and column when the parser knows them */
fun describeConfigError(error: Throwable): String = when (error) {
    is YamlException -> "line ${error.line}, column ${error.column}: ${error.message}"
    else -> error.message ?: error::class.java.simpleName
}
