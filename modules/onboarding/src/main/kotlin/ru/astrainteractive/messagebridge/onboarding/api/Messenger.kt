package ru.astrainteractive.messagebridge.onboarding.api

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding

internal abstract class Messenger<C>(
    val name: String,
    val command: String
) {
    abstract val onboarding: MessengerOnboarding

    abstract fun sectionOf(config: PluginConfiguration): C

    protected abstract fun replaceSection(config: PluginConfiguration, section: C): PluginConfiguration

    protected abstract fun tokenOf(section: C): String

    abstract fun withProxy(section: C, proxy: PluginConfiguration.Proxy?): C

    fun edit(config: PluginConfiguration, change: (C) -> C): PluginConfiguration {
        return replaceSection(config, change.invoke(sectionOf(config)))
    }

    fun hasToken(config: PluginConfiguration): Boolean = tokenOf(sectionOf(config)).isNotBlank()
}
