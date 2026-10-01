package ru.astrainteractive.messagebridge.onboarding.proxy.mapping

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.SetupTranslation

private fun PluginConfiguration.Proxy.describe(): String = "$type $host:$port"

internal fun SetupTranslation.Status.proxyLineOf(configuredProxy: PluginConfiguration.Proxy?): LocalizableComponent {
    if (configuredProxy == null) return noProxy
    return proxy(configuredProxy.describe())
}

internal fun SetupTranslation.Saved.proxyOf(savedProxy: PluginConfiguration.Proxy?): LocalizableComponent {
    if (savedProxy == null) return proxyRemoved
    return proxy(savedProxy.describe())
}
