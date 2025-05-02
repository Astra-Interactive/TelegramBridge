package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messaging.model.BEvent

interface BEventReceiver {
    fun bEvents(scope: CoroutineScope): Flow<BEvent>
}
