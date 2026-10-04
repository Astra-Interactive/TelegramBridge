package ru.astrainteractive.messagebridge.messaging.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messaging.api.BEventReceiver
import ru.astrainteractive.messagebridge.messaging.model.BEvent

class FakeBEventReceiver(
    private val events: Flow<BEvent>
) : BEventReceiver {
    override fun bEvents(scope: CoroutineScope): Flow<BEvent> = events
}
