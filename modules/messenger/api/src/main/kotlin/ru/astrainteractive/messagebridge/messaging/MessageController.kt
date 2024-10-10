package ru.astrainteractive.messagebridge.messaging

import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

interface MessageController {
    suspend fun send(messageEvent: MessageEvent)
}
