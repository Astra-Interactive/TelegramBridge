package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.concurrent.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> RestAction<T>.await(): T = suspendCancellableCoroutine { continuation ->
    queue(continuation::resume, continuation::resumeWithException)
}

internal suspend fun <T> awaitRequest(request: () -> RestAction<T>): Result<T> {
    return runCatching { request().await() }
        .onFailure { error -> if (error is CancellationException) throw error }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { _ -> this@await.cancel() }
    onSuccess(continuation::resume)
    onError(continuation::resumeWithException)
}

internal suspend fun <T> awaitTask(task: () -> Task<T>): Result<T> {
    return runCatching { task().await() }
        .onFailure { error -> if (error is CancellationException) throw error }
}
