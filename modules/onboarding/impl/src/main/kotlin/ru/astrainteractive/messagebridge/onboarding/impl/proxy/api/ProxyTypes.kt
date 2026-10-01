package ru.astrainteractive.messagebridge.onboarding.impl.proxy.api

import ru.astrainteractive.messagebridge.core.api.config.ProxyType

internal interface ProxyTypes {
    val keywords: List<String>

    fun read(keyword: String): Result<ProxyType>
}
