package ru.astrainteractive.messagebridge.onboarding.proxy

import ru.astrainteractive.messagebridge.core.ProxyType

/** The proxy types a messenger connects through, as they are typed after `proxy`. */
internal interface ProxyTypes {
    val keywords: List<String>

    fun read(keyword: String): Result<ProxyType>
}
