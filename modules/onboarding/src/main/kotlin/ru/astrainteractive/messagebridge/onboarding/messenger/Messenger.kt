package ru.astrainteractive.messagebridge.onboarding.messenger

import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.MessengerOnboarding

/**
 * A messenger as the /mb commands see it: its bot and its part [C] of config.yml.
 *
 * @property name the messenger as the admin knows it, e.g. `Telegram`
 * @property command the subcommand of /mb that sets the messenger up, e.g. `telegram`
 */
internal abstract class Messenger<C>(
    val name: String,
    val command: String
) {
    abstract val onboarding: MessengerOnboarding

    abstract fun sectionOf(config: PluginConfiguration): C

    protected abstract fun replaceSection(config: PluginConfiguration, section: C): PluginConfiguration

    protected abstract fun tokenOf(section: C): String

    /** @param proxy `null` for a direct connection */
    abstract fun withProxy(section: C, proxy: PluginConfiguration.Proxy?): C

    fun edit(config: PluginConfiguration, change: (C) -> C): PluginConfiguration {
        return replaceSection(config, change.invoke(sectionOf(config)))
    }

    fun hasToken(config: PluginConfiguration): Boolean = tokenOf(sectionOf(config)).isNotBlank()
}
