package ru.astrainteractive.messagebridge.onboarding.impl.discord.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.impl.api.Messenger

internal class DiscordMessenger(
    override val onboarding: DiscordOnboarding
) : Messenger(name = "Discord", command = "discord") {
    override fun withProxy(config: PluginConfiguration, proxy: PluginConfiguration.Proxy?): PluginConfiguration {
        return config.copy(jdaConfig = config.jdaConfig.copy(proxy = proxy))
    }
}
