package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import ru.astrainteractive.messagebridge.messenger.discord.model.DisallowedIntentsError

internal fun <T> Flow<T>.fallbackOnDisallowedIntents(onFallback: () -> Unit, fallback: () -> Flow<T>): Flow<T> {
    return catch { t ->
        if (t !is DisallowedIntentsError) throw t
        onFallback.invoke()
        emitAll(fallback.invoke())
    }
}
