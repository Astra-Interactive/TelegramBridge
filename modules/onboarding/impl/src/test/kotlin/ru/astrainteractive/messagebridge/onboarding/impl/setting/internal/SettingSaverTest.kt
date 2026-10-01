@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.impl.setting.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.fake.RecordingConsoleKCommandSender
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.fake.FakeMessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.impl.fake.OnboardingFixture
import ru.astrainteractive.messagebridge.onboarding.impl.setting.model.Setting
import ru.astrainteractive.messagebridge.onboarding.impl.status.internal.StatusText
import ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal.TelegramMessenger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SettingSaverTest {
    private val fixture = OnboardingFixture()
    private val saved = fixture.onboardingTranslation.setup.saved
    private val token = "123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw1"
    private val savedToken = saved.token("1234…saw1")

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun saveToken() {
        fixture.editConfig { config -> config.copy(tgConfig = config.tgConfig.copy(token = token)) }
    }

    @Test
    fun GIVEN_token_telegram_rejects_WHEN_console_sets_it_THEN_reads_the_reason() {
        val reason = LocalizedText.shared("Telegram does not know this token")

        fixture.execute("mb telegram token $token")
        fixture.telegram.status.value = MessengerStatus.Failed(reason)

        assertEquals(listOf(plain(savedToken), plain(saved.connectionFailed(reason))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_bot_that_fails_while_the_setting_is_written_WHEN_saved_THEN_reads_the_reason_without_waiting() = runTest {
        val onboarding = FakeMessengerOnboarding()
        val reason = LocalizedText.shared("Telegram does not know this token")
        val translationKrate = DefaultMutableKrate(factory = ::OnboardingTranslation, loader = { null }).asCachedKrate()
        val saver = SettingSaver(
            configKrate = DefaultMutableKrate(
                factory = { Result.success(PluginConfiguration()) },
                loader = { null },
                saver = { _ -> onboarding.status.value = MessengerStatus.Failed(reason) }
            ),
            config = MutableStateFlow(PluginConfiguration()),
            statusText = StatusText(translationKrate),
            connectionTimeout = 20.seconds,
            translationKrate = translationKrate
        )
        val sender = RecordingConsoleKCommandSender(hasAllPermissions = true)
        val setting = Setting<PluginConfiguration.TelegramConfig>(saved = savedToken) { tgConfig ->
            tgConfig.copy(token = token)
        }

        saver.saveAndConnect(sender, TelegramMessenger(onboarding), setting)

        assertEquals(listOf(plain(savedToken), plain(saved.connectionFailed(reason))), sender.messages.map(::plain))
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun GIVEN_bot_that_keeps_connecting_WHEN_console_sets_token_THEN_reads_still_connecting_once_the_wait_is_over() {
        fixture.execute("mb telegram token $token")
        fixture.telegram.status.value = MessengerStatus.Connecting

        fixture.scheduler.advanceTimeBy(19.seconds)
        assertEquals(listOf(plain(savedToken)), fixture.consoleReplies)

        fixture.scheduler.advanceTimeBy(2.seconds)
        assertEquals(listOf(plain(savedToken), plain(saved.stillConnecting)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_bot_that_turns_off_WHEN_console_sets_token_THEN_reads_how_to_set_the_token() {
        fixture.telegram.status.value = MessengerStatus.Connecting

        fixture.execute("mb telegram token $token")
        fixture.telegram.status.value = MessengerStatus.Disabled

        assertEquals(
            listOf(plain(savedToken), "Telegram: not configured — /mb telegram token <token>"),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_same_token_as_saved_WHEN_console_sets_it_THEN_reads_the_current_state_at_once() {
        saveToken()
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")

        fixture.execute("mb telegram token $token")

        assertEquals(listOf(plain(savedToken), plain(saved.connected("@ServerBot"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_bot_without_token_WHEN_console_sets_proxy_THEN_reads_only_that_it_is_saved() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080")
        fixture.scheduler.advanceUntilIdle()

        assertEquals(listOf(plain(saved.proxy("HTTP 1.2.3.4:8080"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_two_changes_the_bot_reconnects_with_WHEN_the_bot_connects_THEN_each_sender_reads_it() {
        saveToken()
        fixture.telegram.status.value = MessengerStatus.Connected("@OldBot")

        fixture.execute("mb telegram proxy http 1.2.3.4 8080")
        fixture.execute("mb telegram api-url http://localhost:8081", fixture.admin)
        fixture.telegram.status.value = MessengerStatus.Connecting
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")

        assertEquals(
            listOf(plain(saved.proxy("HTTP 1.2.3.4:8080")), plain(saved.connected("@ServerBot"))),
            fixture.consoleReplies
        )
        assertEquals(
            listOf(plain(saved.apiUrl("http://localhost:8081")), plain(saved.connected("@ServerBot"))),
            fixture.repliesOf(fixture.admin)
        )
    }

    @Test
    fun GIVEN_config_with_an_error_WHEN_console_sets_token_THEN_nothing_is_saved_and_the_bot_is_not_awaited() {
        val brokenConfig = "tgConfig:\n  max_telegram_message_length: not-a-number\n"
        fixture.configFile.writeText(brokenConfig)

        fixture.execute("mb telegram token $token")
        fixture.scheduler.advanceUntilIdle()

        val reply = fixture.consoleReplies.single()
        assertTrue(reply.startsWith("config.yml has an error, so nothing is saved"), reply)
        assertEquals(brokenConfig, fixture.configFile.readText())
    }
}
