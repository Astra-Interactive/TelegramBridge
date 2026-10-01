package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus

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
