package ru.astrainteractive.messagebridge.messenger.telegram.relay

import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.messaging.BEventConsumer
import ru.astrainteractive.messagebridge.messaging.model.BEvent
import java.util.concurrent.CopyOnWriteArrayList

/** Stands for Minecraft and the other messengers: keeps what the relay passes on. */
internal class RecordingEventChannel(
    logger: Logger
) : BEventConsumer,
    Logger by logger {
    val events: MutableList<BEvent> = CopyOnWriteArrayList()

    override suspend fun consume(bEvent: BEvent) {
        events += bEvent
    }
}
