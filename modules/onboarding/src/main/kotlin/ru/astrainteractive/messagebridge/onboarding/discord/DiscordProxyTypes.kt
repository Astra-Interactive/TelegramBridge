package ru.astrainteractive.messagebridge.onboarding.discord

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.ProxyType
import ru.astrainteractive.messagebridge.onboarding.input.refuse
import ru.astrainteractive.messagebridge.onboarding.proxy.AllProxyTypes
import ru.astrainteractive.messagebridge.onboarding.proxy.ProxyTypes

/** The websocket client JDA reaches the Discord gateway with takes an HTTP proxy only. */
internal class DiscordProxyTypes(
    private val allTypes: ProxyTypes,
    translationKrate: CachedKrate<PluginTranslation>
) : ProxyTypes {
    private val translation by translationKrate

    override val keywords: List<String> = listOf(AllProxyTypes.HTTP)

    override fun read(keyword: String): Result<ProxyType> {
        val type = allTypes.read(keyword).getOrElse { error -> return Result.failure(error) }
        if (type != ProxyType.HTTP) return refuse(translation.setup.socksNotSupported)
        return Result.success(type)
    }
}
