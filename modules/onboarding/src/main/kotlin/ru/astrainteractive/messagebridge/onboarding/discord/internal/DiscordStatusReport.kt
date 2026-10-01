package ru.astrainteractive.messagebridge.onboarding.discord.internal

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.proxy.mapping.proxyLineOf
import ru.astrainteractive.messagebridge.onboarding.status.api.StatusReport
import ru.astrainteractive.messagebridge.onboarding.status.internal.StatusText

internal class DiscordStatusReport(
    private val messenger: DiscordMessenger,
    private val config: StateFlow<PluginConfiguration>,
    private val statusText: StatusText,
    translationKrate: CachedKrate<PluginTranslation>
) : StatusReport {
    private val translation by translationKrate

    private fun channelOf(jdaConfig: PluginConfiguration.JdaConfig): LocalizableComponent {
        if (jdaConfig.channelId.isBlank()) return translation.setup.status.noChannel
        return translation.setup.status.channel(jdaConfig.channelId)
    }

    override fun lines(): List<LocalizableComponent> {
        val jdaConfig = messenger.sectionOf(config.value)
        return listOfNotNull(
            statusText.currentStateOf(messenger),
            channelOf(jdaConfig),
            translation.setup.status.proxyLineOf(jdaConfig.proxy),
            statusText.deliveryErrorOf(messenger)
        )
    }
}
