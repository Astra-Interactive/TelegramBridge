package ru.astrainteractive.messagebridge.onboarding.status.internal

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.onboarding.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

internal class StatusText(translationKrate: CachedKrate<PluginTranslation>) {
    private val translation by translationKrate

    fun stateOf(messenger: Messenger<*>, status: MessengerStatus): LocalizableComponent {
        return when (status) {
            MessengerStatus.Disabled -> translation.setup.status.disabled(messenger.name, messenger.command)
            MessengerStatus.Connecting -> translation.setup.status.connecting(messenger.name)
            is MessengerStatus.Connected -> translation.setup.status.connected(messenger.name, status.botName)
            is MessengerStatus.Failed -> translation.setup.status.failed(messenger.name, status.reason)
        }
    }

    fun currentStateOf(messenger: Messenger<*>): LocalizableComponent {
        return stateOf(messenger, messenger.onboarding.status.value)
    }

    fun deliveryErrorOf(messenger: Messenger<*>): LocalizableComponent? {
        return messenger.onboarding.deliveryError.value?.let(translation.setup.status::deliveryError)
    }
}
