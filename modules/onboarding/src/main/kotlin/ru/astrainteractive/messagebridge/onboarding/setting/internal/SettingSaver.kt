package ru.astrainteractive.messagebridge.onboarding.setting.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.config.describeConfigError
import ru.astrainteractive.messagebridge.onboarding.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.setting.model.Setting
import ru.astrainteractive.messagebridge.onboarding.status.internal.StatusText
import kotlin.time.Duration

internal class SettingSaver(
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    private val config: StateFlow<PluginConfiguration>,
    private val statusText: StatusText,
    private val connectionTimeout: Duration,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate

    private fun <C> write(sender: KAudience, messenger: Messenger<C>, setting: Setting<C>): PluginConfiguration? {
        return configKrate
            .saveAndGet { loaded -> loaded.map { config -> messenger.edit(config, setting.change) } }
            .onFailure { error -> sender.sendMessage(translation.setup.configBroken(describeConfigError(error))) }
            .getOrNull()
    }

    private fun connectionResultOf(messenger: Messenger<*>, status: MessengerStatus): LocalizableComponent {
        return when (status) {
            MessengerStatus.Disabled -> statusText.stateOf(messenger, status)
            MessengerStatus.Connecting -> translation.setup.saved.stillConnecting
            is MessengerStatus.Connected -> translation.setup.saved.connected(status.botName)
            is MessengerStatus.Failed -> translation.setup.saved.connectionFailed(status.reason)
        }
    }

    private suspend fun awaitConnection(
        messenger: Messenger<*>,
        nextStatus: Deferred<MessengerStatus>
    ): MessengerStatus {
        return withTimeoutOrNull(connectionTimeout) { nextStatus.await() } ?: messenger.onboarding.status.value
    }

    fun <C> save(sender: KAudience, messenger: Messenger<C>, setting: Setting<C>) {
        if (write(sender, messenger, setting) == null) return
        sender.sendMessage(setting.saved)
    }

    suspend fun <C> saveAndConnect(sender: KAudience, messenger: Messenger<C>, setting: Setting<C>) = coroutineScope {
        val runningSection = messenger.sectionOf(config.value)
        val nextStatus = async(start = CoroutineStart.UNDISPATCHED) {
            messenger.onboarding.status
                .drop(1)
                .first { status -> status != MessengerStatus.Connecting }
        }
        try {
            val updated = write(sender, messenger, setting) ?: return@coroutineScope
            sender.sendMessage(setting.saved)
            if (!messenger.hasToken(updated)) return@coroutineScope
            val status = if (messenger.sectionOf(updated) != runningSection) {
                awaitConnection(messenger, nextStatus)
            } else {
                messenger.onboarding.status.value
            }
            sender.sendMessage(connectionResultOf(messenger, status))
        } finally {
            nextStatus.cancel()
        }
    }
}
