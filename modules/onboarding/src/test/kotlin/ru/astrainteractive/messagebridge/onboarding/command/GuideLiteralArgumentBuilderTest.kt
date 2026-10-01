@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import ru.astrainteractive.messagebridge.onboarding.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GuideLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val status = fixture.translation.setup.status

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    @Test
    fun GIVEN_telegram_bot_without_token_WHEN_console_runs_telegram_THEN_reads_guide_and_status() {
        fixture.execute("mb telegram")

        assertEquals(
            listOf(
                plain(fixture.translation.telegram.guide),
                "Telegram: not configured — /mb telegram token <token>",
                plain(status.noChat),
                plain(status.noProxy)
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_connected_telegram_bot_WHEN_console_runs_telegram_THEN_reads_status_without_guide() {
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")

        fixture.execute("mb telegram")

        assertEquals(
            listOf("Telegram: connected as @ServerBot", plain(status.noChat), plain(status.noProxy)),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_discord_bot_that_failed_WHEN_console_runs_discord_THEN_reads_guide_and_why() {
        fixture.discord.status.value = MessengerStatus.Failed(LocalizedText.shared("the token is revoked"))

        fixture.execute("mb discord")

        assertEquals(
            listOf(
                plain(fixture.translation.discord.guide),
                "Discord: not connected — the token is revoked",
                plain(status.noChannel),
                plain(status.noProxy)
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_runs_discord_THEN_reads_no_permission() {
        fixture.execute("mb discord", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
