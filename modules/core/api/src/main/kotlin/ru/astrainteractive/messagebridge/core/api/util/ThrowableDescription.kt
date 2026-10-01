package ru.astrainteractive.messagebridge.core.api.util

import com.charleskorn.kaml.YamlException

fun Throwable.describe(): String = when (this) {
    is YamlException -> "line $line, column $column: $message"
    else -> message ?: this::class.java.simpleName
}
