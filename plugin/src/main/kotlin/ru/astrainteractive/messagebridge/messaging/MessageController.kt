package ru.astrainteractive.messagebridge.messaging

import ru.astrainteractive.messagebridge.messaging.model.Message

interface MessageController {
    suspend fun send(message: Message)
}
