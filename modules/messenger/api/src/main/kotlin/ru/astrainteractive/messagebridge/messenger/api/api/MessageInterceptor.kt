package ru.astrainteractive.messagebridge.messenger.api.api

import ru.astrainteractive.messagebridge.messenger.api.model.Interception

fun interface MessageInterceptor<in M> {
    suspend fun intercept(event: M): Interception
}

suspend fun <M> List<MessageInterceptor<M>>.intercept(event: M): Interception {
    for (interceptor in this) {
        val interception = interceptor.intercept(event)
        if (interception != Interception.Pass) return interception
    }
    return Interception.Pass
}
