package ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.connection.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.failure.mapping.TelegramFailureTextMapper

internal class TelegramStatusLogger(
    translationKrate: CachedKrate<PluginTranslation>,
    private val failureTextMapper: TelegramFailureTextMapper,
    logger: Logger,
) : Logger by logger {
    private val translation by translationKrate

    fun log(state: TelegramConnectionState) {
        when (state) {
            TelegramConnectionState.Disabled -> info { translation.telegram.guide.toMessengerText() }
            TelegramConnectionState.Connecting -> verbose { "#log connecting to Telegram" }
            is TelegramConnectionState.Connected -> info {
                translation.telegram.status.connected(state.botName).toMessengerText()
            }

            is TelegramConnectionState.Failed -> error { failureTextMapper.map(state.failure).toMessengerText() }
        }
    }
}
