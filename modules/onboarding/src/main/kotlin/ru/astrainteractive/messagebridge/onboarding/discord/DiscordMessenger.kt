package ru.astrainteractive.messagebridge.onboarding.discord

import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.messenger.Messenger

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
