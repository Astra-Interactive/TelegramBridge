package ru.astrainteractive.messagebridge.messaging.fake

import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.BEvent

class FakeBEventConsumer(
    private val send: suspend (BEvent) -> Unit
) : BEventConsumer {
    val sent = mutableListOf<BEvent>()

    override suspend fun consume(bEvent: BEvent) {
        send.invoke(bEvent)
        sent += bEvent
    }
}
