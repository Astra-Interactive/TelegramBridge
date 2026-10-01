package ru.astrainteractive.messagebridge.onboarding.discord.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding

internal class DiscordMessenger(
    override val onboarding: DiscordOnboarding
) : Messenger<PluginConfiguration.JdaConfig>(name = "Discord", command = "discord") {
    override fun sectionOf(config: PluginConfiguration): PluginConfiguration.JdaConfig = config.jdaConfig

    override fun replaceSection(
        config: PluginConfiguration,
        section: PluginConfiguration.JdaConfig
    ): PluginConfiguration = config.copy(jdaConfig = section)

    override fun tokenOf(section: PluginConfiguration.JdaConfig): String = section.token

    override fun withProxy(
        section: PluginConfiguration.JdaConfig,
        proxy: PluginConfiguration.Proxy?
    ): PluginConfiguration.JdaConfig = section.copy(proxy = proxy)
}
