@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.onboarding

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.FakeClock
import ru.astrainteractive.messagebridge.messenger.telegram.FakeTranslationKrate
import ru.astrainteractive.messagebridge.messenger.telegram.NOW_SECONDS
import ru.astrainteractive.messagebridge.messenger.telegram.configurationOf
import ru.astrainteractive.messagebridge.messenger.telegram.connection.TelegramConnection
import ru.astrainteractive.messagebridge.messenger.telegram.failure.TelegramFailureTextMapper
import ru.astrainteractive.messagebridge.messenger.telegram.request.FakeTelegramBotApi
import ru.astrainteractive.messagebridge.onboarding.bind.BindCodes
import ru.astrainteractive.messagebridge.onboarding.check.CheckLevel
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class TelegramOnboardingTest {
    private val translation = PluginTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val bindCodes = BindCodes(
        clock = FakeClock(now = Instant.fromEpochSeconds(NOW_SECONDS.toLong())),
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
            connections = flowOf(TelegramConnection.Disabled),
            botApi = FakeTelegramBotApi(),
            failureTextMapper = TelegramFailureTextMapper(translationKrate = translationKrate)
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
