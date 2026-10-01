package ru.astrainteractive.messagebridge.messenger.api.fake

import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messenger.api.api.BEventConsumer
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent
import java.util.concurrent.CopyOnWriteArrayList

class RecordingEventChannel(
    logger: Logger
) : BEventConsumer,
    Logger by logger {
    val events: MutableList<BEvent> = CopyOnWriteArrayList()

    override suspend fun consume(bEvent: BEvent) {
        events += bEvent
    }
}
