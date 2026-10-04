package ru.astrainteractive.messagebridge.messaging.api

import ru.astrainteractive.messagebridge.messaging.model.BEvent.Text

fun interface TextInterceptor {
    suspend fun intercept(text: Text): Text
}
