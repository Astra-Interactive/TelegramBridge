package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal

import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState

internal class TelegramStatusLogger(
    translationKrate: CachedKrate<PluginTranslation>,
    private val failureTextMapper: TelegramFailureTextMapper,
    logger: Logger,
) : Logger by logger {
    private val translation by translationKrate

    fun log(state: TelegramConnectionState) {
        when (state) {
            TelegramConnectionState.Disabled -> verbose { "#log Telegram is disabled: there is no token" }
            TelegramConnectionState.Connecting -> verbose { "#log connecting to Telegram" }
            is TelegramConnectionState.Connected -> info {
                translation.telegram.status.connected(state.botName).toMessengerText()
            }

            is TelegramConnectionState.Failed -> error { failureTextMapper.map(state.failure).toMessengerText() }
        }
    }
}
