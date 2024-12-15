package ru.astrainteractive.messagebridge.messaging

import ru.astrainteractive.messagebridge.messaging.model.BEvent

interface BEventConsumer {
    suspend fun consume(bEvent: BEvent)
}
