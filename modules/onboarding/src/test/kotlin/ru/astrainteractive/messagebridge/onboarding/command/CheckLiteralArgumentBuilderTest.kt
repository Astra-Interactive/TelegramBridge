@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.CheckLevel
import ru.astrainteractive.messagebridge.onboarding.fake.OnboardingFixture
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.translation.setup

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun Component.colorOf(text: String, inheritedColor: TextColor?): TextColor? {
        val color = color() ?: inheritedColor
        if (this is TextComponent && text in content()) return color
        return children().firstNotNullOfOrNull { child -> child.colorOf(text, color) }
    }

    private fun colorOf(message: LocalizableComponent, text: String): TextColor? {
        return message.toComponent(Locale.ROOT).colorOf(text, inheritedColor = null)
    }

    @Test
    fun GIVEN_checks_of_the_telegram_bot_WHEN_console_runs_check_THEN_reads_each_marked_and_colored_by_level() {
        fixture.telegram.checks = listOf(
            Check(CheckLevel.OK, LocalizedText.shared("Token is valid")),
            Check(CheckLevel.WARNING, LocalizedText.shared("Topic is not set")),
            Check(CheckLevel.ERROR, LocalizedText.shared("Bot is not an admin"))
        )

        fixture.execute("mb telegram check")

        assertEquals(
            listOf(
                plain(setup.checkStarted("Telegram")),
                "✔ Token is valid",
                "⚠ Topic is not set",
                "✖ Bot is not an admin"
            ),
            fixture.consoleReplies
        )
        val lines = fixture.console.messages.drop(1)
        assertEquals(
            listOf("#42f596", "#dbbb18", "#db2c18").map(TextColor::fromHexString),
            listOf(
                colorOf(lines[0], "Token is valid"),
                colorOf(lines[1], "Topic is not set"),
                colorOf(lines[2], "Bot is not an admin")
            )
        )
    }

    @Test
    fun GIVEN_discord_bot_WHEN_console_runs_check_THEN_reads_that_discord_is_checked() {
        fixture.discord.checks = listOf(Check(CheckLevel.OK, LocalizedText.shared("Channel is found")))

        fixture.execute("mb discord check")

        assertEquals(listOf(plain(setup.checkStarted("Discord")), "✔ Channel is found"), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_runs_check_THEN_reads_no_permission() {
        fixture.execute("mb discord check", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
