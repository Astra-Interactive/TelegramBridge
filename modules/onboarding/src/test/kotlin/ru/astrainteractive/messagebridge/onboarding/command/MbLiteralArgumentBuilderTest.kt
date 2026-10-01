@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.command

import ru.astrainteractive.messagebridge.onboarding.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MbLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    @Test
    fun GIVEN_player_without_permissions_WHEN_runs_mb_THEN_reads_the_commands() {
        fixture.execute("mb", fixture.steve)

        val help = fixture.repliesOf(fixture.steve).single()
        assertTrue("/mb telegram token <token>" in help, help)
        assertTrue("/mb telegram proxy <http|socks5> <host> <port> [username] [password]" in help, help)
        assertTrue("/mb discord invite" in help, help)
        assertTrue("--unsafe" in help, help)
    }

    @Test
    fun GIVEN_player_without_permissions_WHEN_runs_mb_help_THEN_reads_the_same_commands() {
        fixture.execute("mb help", fixture.steve)

        assertEquals(listOf(fixture.plainTextOf(fixture.translation.setup.help)), fixture.repliesOf(fixture.steve))
    }

    @Test
    fun GIVEN_console_WHEN_runs_mb_THEN_reads_where_every_command_is_described() {
        fixture.execute("mb")

        val help = fixture.consoleReplies.single()
        assertTrue("https://github.com/Astra-Interactive/TelegramBridge/blob/master/docs/commands.md" in help, help)
    }
}
