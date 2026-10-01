package ru.astrainteractive.messagebridge.link

sealed interface UnlinkResponse {
    data object Unlinked : UnlinkResponse

    data object NotLinked : UnlinkResponse

    data object UnknownError : UnlinkResponse
}
