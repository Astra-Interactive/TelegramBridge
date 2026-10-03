package ru.astrainteractive.messagebridge.messaging.fake

import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.api.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.BEvent

internal class FakeBEventConsumer(
    private val send: suspend (BEvent) -> Unit
) : BEventConsumer,
    Logger by JUtiltLogger("MessageBridge-FakeBEventConsumer") {
    val sent = mutableListOf<BEvent>()

    override suspend fun consume(bEvent: BEvent) {
        send.invoke(bEvent)
        sent += bEvent
    }
}
