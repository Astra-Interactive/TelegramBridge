package ru.astrainteractive.messagebridge.onboarding.telegram

import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.messenger.Messenger

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
