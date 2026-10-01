package ru.astrainteractive.messagebridge.link

import ru.astrainteractive.messagebridge.link.player.LinkedPlayerModel

sealed interface LinkResponse {
    data object AlreadyLinked : LinkResponse

    data object NoCode : LinkResponse

    data object NoUsername : LinkResponse

    data object UnknownError : LinkResponse

    data class Linked(val user: LinkedPlayerModel) : LinkResponse
}
