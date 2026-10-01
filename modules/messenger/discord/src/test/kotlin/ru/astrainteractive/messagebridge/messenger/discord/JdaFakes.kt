package ru.astrainteractive.messagebridge.messenger.discord

import net.dv8tion.jda.api.requests.RestAction
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.function.Consumer

/**
 * A JDA interface that answers only what a test needs. JDA entities have hundreds of methods, so a hand-written
 * fake is not practical; any other call returns `null`.
 */
internal inline fun <reified T : Any> jdaFake(crossinline answer: (method: Method, args: List<Any?>) -> Any?): T {
    val fake = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
        when (method.name) {
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            "toString" -> "${T::class.java.simpleName}Fake"
            else -> answer(method, args?.toList().orEmpty())
        }
    }
    return fake as T
}

/** A request that JDA completes with [value] or fails with [failure]. */
@Suppress("UNCHECKED_CAST")
internal inline fun <reified T : RestAction<*>> jdaRequest(value: Any? = null, failure: Throwable? = null): T {
    return jdaFake { method, args ->
        if (method.name == "queue" && args.size == 2) {
            if (failure == null) {
                (args[0] as Consumer<Any?>).accept(value)
            } else {
                (args[1] as Consumer<Throwable>).accept(failure)
            }
        }
        null
    }
}
