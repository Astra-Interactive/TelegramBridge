package ru.astrainteractive.messagebridge.onboarding.proxy

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.ProxyType
import ru.astrainteractive.messagebridge.onboarding.input.refuse

/** Every proxy type the plugin can connect through; `socks` is taken for [SOCKS5] as well. */
internal class AllProxyTypes(translationKrate: CachedKrate<PluginTranslation>) : ProxyTypes {
    private val translation by translationKrate

    override val keywords: List<String> = listOf(HTTP, SOCKS5)

    override fun read(keyword: String): Result<ProxyType> {
        return when (keyword.lowercase()) {
            HTTP -> Result.success(ProxyType.HTTP)
            SOCKS5, SOCKS -> Result.success(ProxyType.SOCKS5)
            else -> refuse(translation.setup.invalidProxyType)
        }
    }

    companion object {
        const val HTTP = "http"
        const val SOCKS5 = "socks5"
        private const val SOCKS = "socks"
    }
}
