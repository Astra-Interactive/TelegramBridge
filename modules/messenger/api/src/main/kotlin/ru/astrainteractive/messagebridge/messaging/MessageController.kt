package ru.astrainteractive.messagebridge.messaging

import ru.astrainteractive.messagebridge.messaging.model.ServerEvent

interface MessageController {
    suspend fun send(serverEvent: ServerEvent)
}
