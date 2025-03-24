package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object RestActionExt {
    suspend fun <T> RestAction<T>.await() = supervisorScope {
        suspendCancellableCoroutine<T> { continuation ->
            queue(continuation::resume, continuation::resumeWithException)
        }
    }

    suspend fun <T> RestAction<T>.awaitCatching() = supervisorScope {
        kotlin.runCatching {
            suspendCancellableCoroutine<T> { continuation ->
                queue(continuation::resume, continuation::resumeWithException)
            }
        }
    }

    suspend fun <T> RestAction<T>.async(): Deferred<T> = supervisorScope {
        async { await() }
    }

    suspend fun <T> RestAction<T>.asyncResult(): Deferred<Result<T>> = supervisorScope {
        async { kotlin.runCatching { await() } }
    }
}
