package ru.astrainteractive.messagebridge.onboarding.telegram

import kotlinx.coroutines.flow.StateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.proxy.proxyLineOf
import ru.astrainteractive.messagebridge.onboarding.secret.withoutCredentials
import ru.astrainteractive.messagebridge.onboarding.status.StatusReport
import ru.astrainteractive.messagebridge.onboarding.status.StatusText

/** The bot, the chat and the topic it writes to, its proxy and its Bot API server. */
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

    /** @return `null` for api.telegram.org */
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
