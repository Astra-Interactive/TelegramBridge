package ru.astrainteractive.messagebridge.commands.setup

import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.setup.MessengerSetup

/** A messenger as the /mb commands see it: its bot and its part of config.yml. */
internal class SetupMessenger<C>(
    val name: String,
    val command: String,
    val setup: MessengerSetup,
    private val readSection: (PluginConfiguration) -> C,
    private val replaceSection: (PluginConfiguration, C) -> PluginConfiguration,
    private val tokenOf: (C) -> String
) {
    fun sectionOf(config: PluginConfiguration): C = readSection.invoke(config)

    fun edit(config: PluginConfiguration, block: (C) -> C): PluginConfiguration {
        return replaceSection.invoke(config, block.invoke(sectionOf(config)))
    }

    fun hasToken(config: PluginConfiguration): Boolean = tokenOf.invoke(sectionOf(config)).isNotBlank()

    companion object {
        fun telegram(setup: MessengerSetup): SetupMessenger<PluginConfiguration.TelegramConfig> = SetupMessenger(
            name = "Telegram",
            command = "telegram",
            setup = setup,
            readSection = PluginConfiguration::tgConfig,
            replaceSection = { config, tgConfig -> config.copy(tgConfig = tgConfig) },
            tokenOf = PluginConfiguration.TelegramConfig::token
        )

        fun discord(setup: MessengerSetup): SetupMessenger<PluginConfiguration.JdaConfig> = SetupMessenger(
            name = "Discord",
            command = "discord",
            setup = setup,
            readSection = PluginConfiguration::jdaConfig,
            replaceSection = { config, jdaConfig -> config.copy(jdaConfig = jdaConfig) },
            tokenOf = PluginConfiguration.JdaConfig::token
        )
    }
}
