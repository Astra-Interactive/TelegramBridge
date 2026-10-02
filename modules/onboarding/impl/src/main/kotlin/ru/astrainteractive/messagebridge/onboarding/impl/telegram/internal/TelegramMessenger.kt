package ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.impl.api.Messenger

internal class TelegramMessenger(
    override val onboarding: MessengerOnboarding
) : Messenger(name = "Telegram", command = "telegram") {
    override fun withProxy(config: PluginConfiguration, proxy: PluginConfiguration.Proxy?): PluginConfiguration {
        return config.copy(tgConfig = config.tgConfig.copy(proxy = proxy))
    }
}
