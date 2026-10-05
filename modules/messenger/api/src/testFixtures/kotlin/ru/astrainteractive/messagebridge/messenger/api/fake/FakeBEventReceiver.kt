package ru.astrainteractive.messagebridge.messenger.api.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messenger.api.api.BEventReceiver
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

class FakeBEventReceiver(
    private val events: Flow<BEvent>
) : BEventReceiver {
    override fun bEvents(scope: CoroutineScope): Flow<BEvent> = events
}
