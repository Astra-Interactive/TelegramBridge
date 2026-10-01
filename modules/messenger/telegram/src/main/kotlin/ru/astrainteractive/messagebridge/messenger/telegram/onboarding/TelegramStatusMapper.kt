package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import ru.astrainteractive.messagebridge.messenger.telegram.connection.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus

internal class TelegramStatusMapper(
    private val failureTextMapper: TelegramFailureTextMapper,
) {
    fun map(state: TelegramConnectionState): MessengerStatus = when (state) {
        TelegramConnectionState.Disabled -> MessengerStatus.Disabled
        TelegramConnectionState.Connecting -> MessengerStatus.Connecting
        is TelegramConnectionState.Connected -> MessengerStatus.Connected(state.botName)
        is TelegramConnectionState.Failed -> MessengerStatus.Failed(failureTextMapper.lazyMap(state.failure))
    }
}
