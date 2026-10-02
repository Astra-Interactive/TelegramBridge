package ru.astrainteractive.messagebridge.onboarding.impl.discord.internal

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.ProxyType
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.impl.model.refuse
import ru.astrainteractive.messagebridge.onboarding.impl.proxy.api.ProxyTypes
import ru.astrainteractive.messagebridge.onboarding.impl.proxy.internal.AllProxyTypes

internal class DiscordProxyTypes(
    private val allTypes: ProxyTypes,
    translationKrate: CachedKrate<OnboardingTranslation>
) : ProxyTypes {
    private val translation by translationKrate

    override val keywords: List<String> = listOf(AllProxyTypes.HTTP)

    override fun read(keyword: String): Result<ProxyType> {
        val type = allTypes.read(keyword).getOrElse { t -> return Result.failure(t) }
        if (type != ProxyType.HTTP) return refuse(translation.setup.socksNotSupported)
        return Result.success(type)
    }
}
