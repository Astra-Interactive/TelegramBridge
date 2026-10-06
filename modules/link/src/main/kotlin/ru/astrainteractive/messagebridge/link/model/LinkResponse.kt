package ru.astrainteractive.messagebridge.link.model

internal sealed interface LinkResponse {
    data object Linked : LinkResponse
    data object AlreadyLinked : LinkResponse
    data object AccountTaken : LinkResponse
    data object NoCode : LinkResponse
    data object UnknownError : LinkResponse
}
