package ru.astrainteractive.messagebridge.messenger.discord.api.util

import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.concurrent.Task
import ru.astrainteractive.klibs.mikro.core.coroutines.propagateCancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> RestAction<T>.await(): T = suspendCancellableCoroutine { continuation ->
    queue(continuation::resume, continuation::resumeWithException)
}

suspend fun <T> awaitRequest(request: () -> RestAction<T>): Result<T> {
    return runCatching { request.invoke().await() }
        .propagateCancellationException()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { _ -> this@await.cancel() }
    onSuccess(continuation::resume)
    onError(continuation::resumeWithException)
}

suspend fun <T> awaitTask(task: () -> Task<T>): Result<T> {
    return runCatching { task.invoke().await() }
        .propagateCancellationException()
}
