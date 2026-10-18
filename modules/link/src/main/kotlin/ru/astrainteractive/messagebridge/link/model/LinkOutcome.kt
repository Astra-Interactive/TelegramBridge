package ru.astrainteractive.messagebridge.link.model

internal sealed interface LinkOutcome {
    data object Linked : LinkOutcome
    data object AlreadyLinked : LinkOutcome
    data object AccountTaken : LinkOutcome
}
