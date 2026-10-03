package ru.astrainteractive.messagebridge.link.command

private const val LINK_COMMAND = "/link"
private const val INVALID_LINK_CODE = -1

internal fun String.toLinkCode(): Int? {
    if (!startsWith(LINK_COMMAND)) return null
    return replace("$LINK_COMMAND ", "").toIntOrNull() ?: INVALID_LINK_CODE
}
