package ru.astrainteractive.messagebridge.messenger.discord.fake

import java.lang.reflect.Proxy

inline fun <reified T : Any> jdaFake(answerByMethod: Map<String, Any?>): T {
    val fake = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        check(method.name in answerByMethod) { "${T::class.simpleName}.${method.name} is not faked" }
        val answer = answerByMethod[method.name]
        if (answer is JdaAnswer) answer.answer(args.orEmpty().toList()) else answer
    }
    return fake as T
}
