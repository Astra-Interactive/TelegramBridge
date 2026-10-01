@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.onboarding.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.connection.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.failure.mapping.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.failure.model.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TelegramStatusMapperTest {
    private val translation = PluginTranslation()
    private val mapper = TelegramStatusMapper(
        failureTextMapper = TelegramFailureTextMapper(translationKrate = FakeTranslationKrate(translation))
    )

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
        assertEquals(translation.telegram.errors.tokenInUse.toMessengerText(), reason.toMessengerText())
    }
}
