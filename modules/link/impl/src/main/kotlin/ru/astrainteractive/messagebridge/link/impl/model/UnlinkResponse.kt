package ru.astrainteractive.messagebridge.link.impl.model

internal sealed interface UnlinkResponse {
    data object Unlinked : UnlinkResponse

    data object NotLinked : UnlinkResponse

    data object UnknownError : UnlinkResponse
}
