@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.fake.FakeClock
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.util.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.FakeTelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.NOW
import ru.astrainteractive.messagebridge.messenger.telegram.api.fake.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.api.model.TelegramConnectionState
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.impl.BindCodes
import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class TelegramOnboardingTest {
    private val translation = OnboardingTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val bindCodes = BindCodes(
        clock = FakeClock(now = NOW),
        lifetime = 10.minutes,
        random = Random(1)
    )
    private val onboarding = TelegramOnboarding(
        status = MutableStateFlow(MessengerStatus.Connecting),
        deliveryError = MutableStateFlow<LocalizableComponent?>(null),
        bindCodes = bindCodes,
        diagnostics = TelegramDiagnostics(
            configFlow = MutableStateFlow(configurationOf()),
            translationKrate = translationKrate,
            connectionState = MutableStateFlow(TelegramConnectionState.Disabled),
            botApi = FakeTelegramBotApi(),
            failureTextMapper = FakeTelegramFailureTextMapper()
        )
    )

    @Test
    fun GIVEN_admin_asks_for_a_code_WHEN_it_is_issued_THEN_the_chat_can_be_bound_with_it() {
        val code = onboarding.issueBindCode(onBound = { _ -> })

        assertTrue(bindCodes.isValid(code.value))
        assertEquals(10.minutes, code.lifetime)
    }

    @Test
    fun GIVEN_no_token_WHEN_admin_runs_the_check_THEN_the_telegram_checks_answer() = runTest {
        val checks = onboarding.check()

        assertEquals(CheckLevel.ERROR, checks.single().level)
        val tokenMissing = translation.telegram.check.tokenMissing
        assertEquals(tokenMissing.toMessengerText(), checks.single().message.toMessengerText())
    }
}
