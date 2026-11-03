package ru.astrainteractive.messagebridge.messenger.discord.fake

import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Response

fun errorResponseExceptionFake(errorResponse: ErrorResponse, status: Int, message: String): ErrorResponseException {
    return Response(
        null,
        status,
        message,
        -1,
        emptySet()
    ).use { response -> ErrorResponseException.create(errorResponse, response) }
}
