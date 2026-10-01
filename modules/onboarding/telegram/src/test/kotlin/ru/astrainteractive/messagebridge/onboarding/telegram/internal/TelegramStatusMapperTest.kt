@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramFailure
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TelegramStatusMapperTest {
    private val failureTextMapper = FakeTelegramFailureTextMapper()
    private val mapper = TelegramStatusMapper(failureTextMapper = failureTextMapper)

    @Test
    fun GIVEN_states_without_details_WHEN_mapped_THEN_status_is_the_same() {
        assertEquals(MessengerStatus.Disabled, mapper.map(TelegramConnectionState.Disabled))
        assertEquals(MessengerStatus.Connecting, mapper.map(TelegramConnectionState.Connecting))
    }

    @Test
    fun GIVEN_connected_bot_WHEN_mapped_THEN_status_names_the_bot() {
        val status = mapper.map(TelegramConnectionState.Connected("@MyBridgeBot"))

        assertEquals(MessengerStatus.Connected("@MyBridgeBot"), status)
    }

    @Test
    fun GIVEN_failed_bot_WHEN_mapped_THEN_status_tells_what_to_do() {
        val status = mapper.map(TelegramConnectionState.Failed(TelegramFailure.TokenInUse))

        val reason = assertIs<MessengerStatus.Failed>(status).reason
        assertEquals(failureTextMapper.map(TelegramFailure.TokenInUse).toMessengerText(), reason.toMessengerText())
    }
}
