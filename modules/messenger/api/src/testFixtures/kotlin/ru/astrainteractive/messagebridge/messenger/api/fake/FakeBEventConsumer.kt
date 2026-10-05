package ru.astrainteractive.messagebridge.messenger.api.fake

import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

class FakeBEventConsumer(
    private val send: suspend (BEvent) -> Unit
) : BEventConsumer {
    val sent = mutableListOf<BEvent>()

    override suspend fun consume(bEvent: BEvent) {
        send.invoke(bEvent)
        sent += bEvent
    }
}
