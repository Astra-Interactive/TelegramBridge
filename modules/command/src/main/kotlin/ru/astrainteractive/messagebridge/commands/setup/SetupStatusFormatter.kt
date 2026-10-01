package ru.astrainteractive.messagebridge.commands.setup

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messaging.setup.DiagnosticCheck
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus

internal class SetupStatusFormatter(translationKrate: CachedKrate<PluginTranslation>) {
    private val translation by translationKrate

    fun stateOf(messenger: SetupMessenger<*>, status: MessengerStatus): LocalizableComponent {
        return when (status) {
            MessengerStatus.Disabled -> translation.setup.status.disabled(messenger.name, messenger.command)
            MessengerStatus.Connecting -> translation.setup.status.connecting(messenger.name)
            is MessengerStatus.Connected -> translation.setup.status.connected(messenger.name, status.botName)
            is MessengerStatus.Failed -> translation.setup.status.failed(messenger.name, status.reason)
        }
    }

    /** Reply to a change the bot reconnects with, once the bot has connected, failed or run out of time. */
    fun connectionResultOf(messenger: SetupMessenger<*>, status: MessengerStatus): LocalizableComponent {
        return when (status) {
            MessengerStatus.Disabled -> stateOf(messenger, status)
            MessengerStatus.Connecting -> translation.setup.saved.stillConnecting
            is MessengerStatus.Connected -> translation.setup.saved.connected(status.botName)
            is MessengerStatus.Failed -> translation.setup.saved.connectionFailed(status.reason)
        }
    }

    fun telegram(
        messenger: SetupMessenger<PluginConfiguration.TelegramConfig>,
        config: PluginConfiguration
    ): List<LocalizableComponent> {
        val tgConfig = messenger.sectionOf(config)
        val chat = when {
            tgConfig.chatID.isBlank() -> translation.setup.status.noChat
            tgConfig.topicID.isBlank() -> translation.setup.status.chat(tgConfig.chatID)
            else -> translation.setup.status.chatWithTopic(tgConfig.chatID, tgConfig.topicID)
        }
        return listOfNotNull(
            stateOf(messenger, messenger.setup.status.value),
            chat,
            proxyOf(tgConfig.proxy),
            tgConfig.apiUrl
                .takeIf(String::isNotBlank)
                ?.let { url -> translation.setup.status.apiUrl(withoutCredentials(url)) },
            deliveryErrorOf(messenger)
        )
    }

    fun discord(
        messenger: SetupMessenger<PluginConfiguration.JdaConfig>,
        config: PluginConfiguration
    ): List<LocalizableComponent> {
        val jdaConfig = messenger.sectionOf(config)
        return listOfNotNull(
            stateOf(messenger, messenger.setup.status.value),
            jdaConfig.channelId
                .takeIf(String::isNotBlank)
                ?.let(translation.setup.status::channel)
                ?: translation.setup.status.noChannel,
            proxyOf(jdaConfig.proxy),
            deliveryErrorOf(messenger)
        )
    }

    /** The texts of a check are plain, so the mark and the color of its level are added here. */
    fun checkLineOf(check: DiagnosticCheck): LocalizableComponent {
        return when (check.level) {
            DiagnosticCheck.Level.OK -> translation.setup.checkOk(check.message)
            DiagnosticCheck.Level.WARNING -> translation.setup.checkWarning(check.message)
            DiagnosticCheck.Level.ERROR -> translation.setup.checkError(check.message)
        }
    }

    private fun proxyOf(proxy: PluginConfiguration.Proxy?): LocalizableComponent {
        return proxy?.let { translation.setup.status.proxy(proxy.describe()) } ?: translation.setup.status.noProxy
    }

    private fun deliveryErrorOf(messenger: SetupMessenger<*>): LocalizableComponent? {
        return messenger.setup.deliveryError.value?.let(translation.setup.status::deliveryError)
    }
}
