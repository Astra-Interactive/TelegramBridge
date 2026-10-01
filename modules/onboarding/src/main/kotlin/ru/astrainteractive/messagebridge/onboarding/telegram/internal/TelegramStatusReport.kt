package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.proxy.mapping.proxyLineOf
import ru.astrainteractive.messagebridge.onboarding.secret.internal.withoutCredentials
import ru.astrainteractive.messagebridge.onboarding.status.api.StatusReport
import ru.astrainteractive.messagebridge.onboarding.status.internal.StatusText

internal class TelegramStatusReport(
    private val messenger: TelegramMessenger,
    private val config: StateFlow<PluginConfiguration>,
    private val statusText: StatusText,
    translationKrate: CachedKrate<PluginTranslation>
) : StatusReport {
    private val translation by translationKrate

    private fun chatOf(tgConfig: PluginConfiguration.TelegramConfig): LocalizableComponent {
        return when {
            tgConfig.chatID.isBlank() -> translation.setup.status.noChat
            tgConfig.topicID.isBlank() -> translation.setup.status.chat(tgConfig.chatID)
            else -> translation.setup.status.chatWithTopic(tgConfig.chatID, tgConfig.topicID)
        }
    }

    private fun apiUrlOf(tgConfig: PluginConfiguration.TelegramConfig): LocalizableComponent? {
        return tgConfig.apiUrl
            .takeIf(String::isNotBlank)
            ?.let { url -> translation.setup.status.apiUrl(withoutCredentials(url)) }
    }

    override fun lines(): List<LocalizableComponent> {
        val tgConfig = messenger.sectionOf(config.value)
        return listOfNotNull(
            statusText.currentStateOf(messenger),
            chatOf(tgConfig),
            translation.setup.status.proxyLineOf(tgConfig.proxy),
            apiUrlOf(tgConfig),
            statusText.deliveryErrorOf(messenger)
        )
    }
}
