@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.bind

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.onboarding.OnboardingFixture
import ru.astrainteractive.messagebridge.onboarding.fake.FakeMessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class BindLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.translation.setup
    private val code = FakeMessengerOnboarding.BIND_CODE

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    @Test
    fun GIVEN_connected_telegram_bot_WHEN_console_asks_for_bind_code_THEN_reads_how_to_send_it_and_then_the_result() {
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")
        val bound = LocalizedText.shared("Chat -1001234567890 is bound")

        fixture.execute("mb telegram bind")
        fixture.telegram.bindCallbacks.single().invoke(bound)

        assertEquals(
            listOf(plain(setup.telegramBindIssued("/bind@ServerBot $code", 10.minutes)), plain(bound)),
            fixture.consoleReplies
        )
        assertTrue("Send /bind@ServerBot $code" in fixture.consoleReplies.first())
        assertTrue("valid for 10 minutes" in fixture.consoleReplies.first())
    }

    @Test
    fun GIVEN_telegram_bot_named_without_at_WHEN_console_asks_for_bind_code_THEN_reads_the_bot_named_once() {
        fixture.telegram.status.value = MessengerStatus.Connected("ServerBot")

        fixture.execute("mb telegram bind")

        assertTrue("Send /bind@ServerBot $code" in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_telegram_bot_that_is_still_connecting_WHEN_console_asks_for_bind_code_THEN_reads_bind_without_bot_name() {
        fixture.telegram.status.value = MessengerStatus.Connecting

        fixture.execute("mb telegram bind")

        assertTrue("Send /bind $code to the Telegram chat" in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_telegram_bot_without_token_WHEN_console_asks_for_bind_code_THEN_reads_how_to_set_the_token() {
        fixture.execute("mb telegram bind")

        assertEquals(listOf("Telegram: not configured — /mb telegram token <token>"), fixture.consoleReplies)
        assertTrue(fixture.telegram.bindCallbacks.isEmpty())
    }

    @Test
    fun GIVEN_discord_bot_that_failed_WHEN_console_asks_for_bind_code_THEN_reads_the_discord_command() {
        fixture.discord.status.value = MessengerStatus.Failed(LocalizedText.shared("no gateway"))

        fixture.execute("mb discord bind")

        assertEquals(listOf(plain(setup.discordBindIssued(code, 10.minutes))), fixture.consoleReplies)
        assertTrue("!bind $code" in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_discord_bot_without_token_WHEN_console_asks_for_bind_code_THEN_reads_how_to_set_the_token() {
        fixture.execute("mb discord bind")

        assertEquals(listOf("Discord: not configured — /mb discord token <token>"), fixture.consoleReplies)
        assertTrue(fixture.discord.bindCallbacks.isEmpty())
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_asks_for_bind_code_THEN_reads_no_permission_and_no_code_is_issued() {
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")

        fixture.execute("mb telegram bind", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
        assertTrue(fixture.telegram.bindCallbacks.isEmpty())
    }
}
