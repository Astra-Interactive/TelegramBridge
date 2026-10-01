@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.commands.setup

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MbLiteralArgumentBuilderTest {
    private val fixture = SetupCommandFixture()
    private val setup = fixture.translation.setup

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    @Test
    fun GIVEN_player_without_permissions_WHEN_runs_mb_THEN_reads_the_commands() {
        fixture.execute("mb", fixture.steve)

        val help = fixture.repliesOf(fixture.steve).single()
        assertTrue("/mb telegram token <token>" in help, help)
        assertTrue("/mb telegram proxy <http|socks5> <host> <port> [username] [password]" in help, help)
        assertTrue("/mb discord invite" in help, help)
    }

    @Test
    fun GIVEN_player_without_permissions_WHEN_runs_status_THEN_reads_no_permission() {
        fixture.execute("mb status", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }

    @Test
    fun GIVEN_bots_in_every_state_WHEN_console_runs_status_THEN_reads_each_bot_and_where_it_writes() {
        fixture.configKrate.save { config ->
            config.copy(
                tgConfig = config.tgConfig.copy(
                    chatID = "-1001234567890",
                    topicID = "12",
                    proxy = PluginConfiguration.Proxy(
                        type = PluginConfiguration.Proxy.Type.SOCKS5,
                        host = "1.2.3.4",
                        port = 1080,
                        username = "user",
                        password = "hunter2"
                    )
                )
            )
        }
        val reason = LocalizedText.shared("the token is revoked")
        val deliveryError = LocalizedText.shared("the bot can not write to the channel")
        fixture.telegramSetup.status.value = MessengerStatus.Connected("@ServerBot")
        fixture.discordSetup.status.value = MessengerStatus.Failed(reason)
        fixture.discordSetup.deliveryError.value = deliveryError

        fixture.execute("mb status")

        assertEquals(
            listOf(
                plain(setup.status.header),
                "Telegram: connected as @ServerBot",
                "  Chat: -1001234567890, topic 12",
                "  Proxy: SOCKS5 1.2.3.4:1080",
                "Discord: not connected — the token is revoked",
                plain(setup.status.noChannel),
                plain(setup.status.noProxy),
                plain(setup.status.deliveryError(deliveryError))
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_bots_without_tokens_WHEN_console_runs_status_THEN_reads_how_to_set_each_token() {
        fixture.execute("mb status")

        assertTrue("Telegram: not configured — /mb telegram token <token>" in fixture.consoleReplies)
        assertTrue("Discord: not configured — /mb discord token <token>" in fixture.consoleReplies)
    }

    @Test
    fun GIVEN_working_files_WHEN_admin_reloads_THEN_reads_reload_completed() {
        fixture.execute("mb reload", fixture.admin)

        assertEquals(
            listOf(plain(fixture.translation.reload.started), plain(fixture.translation.reload.completed)),
            fixture.repliesOf(fixture.admin)
        )
    }

    @Test
    fun GIVEN_config_with_an_error_WHEN_admin_reloads_THEN_reads_the_error_and_that_previous_settings_are_kept() {
        fixture.configFile.file.writeText("tgConfig:\n  max_telegram_message_length: not-a-number\n")

        fixture.execute("mb reload", fixture.admin)

        val replies = fixture.repliesOf(fixture.admin)
        assertEquals(2, replies.size, "$replies")
        assertTrue(replies.last().startsWith("config.yml has an error and is not applied"), replies.last())
        assertTrue("line 2" in replies.last(), replies.last())
    }

    @Test
    fun GIVEN_player_without_reload_permission_WHEN_reloads_THEN_reads_no_permission() {
        fixture.execute("mb reload", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
