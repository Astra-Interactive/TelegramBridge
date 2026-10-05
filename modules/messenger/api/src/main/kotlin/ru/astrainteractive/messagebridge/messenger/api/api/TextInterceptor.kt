package ru.astrainteractive.messagebridge.messenger.api.api

import ru.astrainteractive.messagebridge.messenger.api.model.Text

fun interface TextInterceptor {
    suspend fun intercept(text: Text): Text
}
