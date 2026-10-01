package ru.astrainteractive.messagebridge.link.model

sealed interface UnlinkResponse {
    data object Unlinked : UnlinkResponse

    data object NotLinked : UnlinkResponse

    data object UnknownError : UnlinkResponse
}
