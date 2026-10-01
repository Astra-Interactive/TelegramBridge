package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding

internal class TelegramMessenger(
    override val onboarding: MessengerOnboarding
) : Messenger<PluginConfiguration.TelegramConfig>(name = "Telegram", command = "telegram") {
    override fun sectionOf(config: PluginConfiguration): PluginConfiguration.TelegramConfig = config.tgConfig

    override fun replaceSection(
        config: PluginConfiguration,
        section: PluginConfiguration.TelegramConfig
    ): PluginConfiguration = config.copy(tgConfig = section)

    override fun tokenOf(section: PluginConfiguration.TelegramConfig): String = section.token

    override fun withProxy(
        section: PluginConfiguration.TelegramConfig,
        proxy: PluginConfiguration.Proxy?
    ): PluginConfiguration.TelegramConfig = section.copy(proxy = proxy)
}
