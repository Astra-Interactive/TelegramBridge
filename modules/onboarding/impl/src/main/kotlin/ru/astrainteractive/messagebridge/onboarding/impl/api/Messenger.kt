package ru.astrainteractive.messagebridge.onboarding.impl.api

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding

internal abstract class Messenger(
    val name: String,
    val command: String
) {
    abstract val onboarding: MessengerOnboarding

    abstract fun withProxy(config: PluginConfiguration, proxy: PluginConfiguration.Proxy?): PluginConfiguration
}
