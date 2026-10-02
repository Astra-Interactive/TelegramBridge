@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.flow.MutableStateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.NOW
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class TelegramOnboardingTest {
    private val bindCodes = BindCodes(
        clock = FakeClock(now = NOW),
        lifetime = 10.minutes,
        random = Random(1)
    )
    private val onboarding = TelegramOnboarding(
        status = MutableStateFlow(MessengerStatus.Connecting),
        deliveryError = MutableStateFlow<LocalizableComponent?>(null),
        bindCodes = bindCodes
    )

    @Test
    fun GIVEN_admin_asks_for_a_code_WHEN_it_is_issued_THEN_the_chat_can_be_bound_with_it() {
        val code = onboarding.issueBindCode(onBound = { _ -> })

        assertTrue(bindCodes.isValid(code.value))
        assertEquals(10.minutes, code.lifetime)
    }
}
