package ru.astrainteractive.messagebridge.messenger.discord.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import ru.astrainteractive.messagebridge.messenger.discord.model.DiscordRequestCancelledError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RestActionExt {
    suspend fun <T> RestAction<T>.await() = supervisorScope {
        suspendCancellableCoroutine<T> { continuation ->
            queue(continuation::resume) { t ->
                val failure = if (t is CancellationException) DiscordRequestCancelledError(t) else t
                continuation.resumeWithException(failure)
            }
        }
    }
}
