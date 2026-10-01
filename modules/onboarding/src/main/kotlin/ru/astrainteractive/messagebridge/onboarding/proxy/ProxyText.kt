package ru.astrainteractive.messagebridge.onboarding.proxy

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.SetupTranslation

/** The proxy without its username and password, e.g. `HTTP 1.2.3.4:8080`. */
private fun PluginConfiguration.Proxy.describe(): String = "$type $host:$port"

/** @param configuredProxy `null` for a direct connection */
internal fun SetupTranslation.Status.proxyLineOf(configuredProxy: PluginConfiguration.Proxy?): LocalizableComponent {
    if (configuredProxy == null) return noProxy
    return proxy(configuredProxy.describe())
}

/** @param savedProxy `null` once the proxy is removed */
internal fun SetupTranslation.Saved.proxyOf(savedProxy: PluginConfiguration.Proxy?): LocalizableComponent {
    if (savedProxy == null) return proxyRemoved
    return proxy(savedProxy.describe())
}
