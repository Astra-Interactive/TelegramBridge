package ru.astrainteractive.messagebridge.core.api.config

import com.charleskorn.kaml.YamlException

fun describeConfigError(error: Throwable): String = when (error) {
    is YamlException -> "line ${error.line}, column ${error.column}: ${error.message}"
    else -> error.message ?: error::class.java.simpleName
}
