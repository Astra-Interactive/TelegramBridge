package ru.astrainteractive.messagebridge.messaging

import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messaging.model.BEvent

interface BEventReceiver {
    val bEvents: Flow<BEvent>
}
