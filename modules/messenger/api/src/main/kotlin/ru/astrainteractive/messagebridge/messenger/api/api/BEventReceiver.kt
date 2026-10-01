package ru.astrainteractive.messagebridge.messenger.api.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import ru.astrainteractive.messagebridge.messenger.api.model.BEvent

internal interface BEventReceiver {
    fun bEvents(scope: CoroutineScope): Flow<BEvent>
}
