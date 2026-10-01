package ru.astrainteractive.messagebridge.messenger.discord.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.concurrent.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Throws what JDA fails the request with, e.g. `ErrorResponseException`. */
internal suspend fun <T> RestAction<T>.await(): T = suspendCancellableCoroutine { continuation ->
    queue(continuation::resume, continuation::resumeWithException)
}

/**
 * Makes the request and waits for it. JDA throws `InsufficientPermissionException` while it makes a request the
 * bot has no permission for, so the request is made inside, not passed in. Cancellation is still thrown.
 */
internal suspend fun <T> awaitRequest(request: () -> RestAction<T>): Result<T> {
    return runCatching { request().await() }
        .onFailure { error -> if (error is CancellationException) throw error }
}

/** A finished task calls back at once, so the cancellation is set up before the callbacks. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { _ -> this@await.cancel() }
    onSuccess(continuation::resume)
    onError(continuation::resumeWithException)
}

/**
 * [awaitRequest] for a gateway task, such as loading the members of a server. JDA throws while it makes a task
 * that needs an intent the bot did not ask for.
 */
internal suspend fun <T> awaitTask(task: () -> Task<T>): Result<T> {
    return runCatching { task().await() }
        .onFailure { error -> if (error is CancellationException) throw error }
}
