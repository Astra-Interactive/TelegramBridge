@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.commands.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.setup.DiagnosticCheck
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramLiteralArgumentBuilderTest {
    private val fixture = SetupCommandFixture()
    private val setup = fixture.translation.setup
    private val token = "123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw1"

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun Component.colorOf(text: String, inheritedColor: TextColor? = null): TextColor? {
        val color = color() ?: inheritedColor
        if (this is TextComponent && text in content()) return color
        return children().firstNotNullOfOrNull { child -> child.colorOf(text, color) }
    }

    @Test
    fun GIVEN_player_without_unsafe_WHEN_sets_token_THEN_is_refused_and_nothing_is_saved() {
        fixture.execute("mb telegram token $token", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals("", fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_player_with_unsafe_WHEN_sets_token_THEN_token_is_saved_without_the_flag() {
        fixture.execute("mb telegram token $token --unsafe", fixture.admin)

        assertEquals(token, fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_console_WHEN_sets_token_and_bot_connects_THEN_reads_bot_name_and_never_the_token() {
        fixture.execute("mb telegram token $token")
        fixture.telegramSetup.status.value = MessengerStatus.Connecting
        fixture.telegramSetup.status.value = MessengerStatus.Connected("@ServerBot")

        assertEquals(token, fixture.savedConfig.tgConfig.token)
        assertEquals(
            listOf(plain(setup.saved.token("1234…saw1")), plain(setup.saved.connected("@ServerBot"))),
            fixture.consoleReplies
        )
        assertTrue(fixture.consoleReplies.none { reply -> "AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw1" in reply })
    }

    @Test
    fun GIVEN_console_with_unsafe_WHEN_sets_token_THEN_flag_is_not_saved() {
        fixture.execute("mb telegram token $token --unsafe")

        assertEquals(token, fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_token_telegram_rejects_WHEN_console_sets_it_THEN_reads_the_reason() {
        val reason = LocalizedText.shared("Telegram does not know this token")

        fixture.execute("mb telegram token $token")
        fixture.telegramSetup.status.value = MessengerStatus.Failed(reason)

        assertEquals(plain(setup.saved.connectionFailed(reason)), fixture.consoleReplies.last())
    }

    @Test
    fun GIVEN_bot_that_keeps_connecting_WHEN_console_sets_token_THEN_reads_still_connecting_after_the_wait() {
        fixture.execute("mb telegram token $token")
        fixture.telegramSetup.status.value = MessengerStatus.Connecting
        fixture.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(plain(setup.saved.token("1234…saw1")), plain(setup.saved.stillConnecting)),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_same_token_as_saved_WHEN_console_sets_it_THEN_reads_the_current_state_at_once() {
        fixture.configKrate.save { config -> config.copy(tgConfig = config.tgConfig.copy(token = token)) }
        fixture.telegramSetup.status.value = MessengerStatus.Connected("@ServerBot")

        fixture.execute("mb telegram token $token")

        assertEquals(plain(setup.saved.connected("@ServerBot")), fixture.consoleReplies.last())
    }

    @Test
    fun GIVEN_value_that_is_not_a_token_WHEN_console_sets_it_THEN_reads_invalid_token() {
        fixture.execute("mb telegram token 123456")

        assertEquals(listOf(plain(setup.invalidTelegramToken)), fixture.consoleReplies)
        assertEquals("", fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_sets_chat_THEN_reads_no_permission() {
        fixture.execute("mb telegram chat -1001234567890", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
        assertEquals("", fixture.savedConfig.tgConfig.chatID)
    }

    @Test
    fun GIVEN_chat_id_WHEN_console_sets_it_THEN_it_is_saved() {
        fixture.execute("mb telegram chat -1001234567890")

        assertEquals("-1001234567890", fixture.savedConfig.tgConfig.chatID)
        assertEquals(listOf(plain(setup.saved.chat("-1001234567890"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_chat_id_that_is_not_a_number_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram chat general")

        assertEquals(listOf(plain(setup.invalidChat)), fixture.consoleReplies)
        assertEquals("", fixture.savedConfig.tgConfig.chatID)
    }

    @Test
    fun GIVEN_config_with_an_error_WHEN_console_sets_chat_THEN_nothing_is_saved_and_file_is_left_as_is() {
        val brokenConfig = "tgConfig:\n  max_telegram_message_length: not-a-number\n"
        fixture.configFile.file.writeText(brokenConfig)

        fixture.execute("mb telegram chat -1001234567890")

        val reply = fixture.consoleReplies.single()
        assertTrue(reply.startsWith("config.yml has an error, so nothing is saved"), reply)
        assertTrue("line 2" in reply, reply)
        assertEquals(brokenConfig, fixture.configFile.file.readText())
    }

    @Test
    fun GIVEN_topic_WHEN_console_sets_it_and_then_none_THEN_topic_is_saved_and_cleared() {
        fixture.execute("mb telegram topic 12")
        assertEquals("12", fixture.savedConfig.tgConfig.topicID)

        fixture.execute("mb telegram topic none")

        assertEquals("", fixture.savedConfig.tgConfig.topicID)
        assertEquals(listOf(plain(setup.saved.topic("12")), plain(setup.saved.topicRemoved)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_topic_that_is_not_a_number_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram topic general")

        assertEquals(listOf(plain(setup.invalidTopic)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_socks5_proxy_WHEN_console_sets_it_THEN_it_is_saved() {
        fixture.execute("mb telegram proxy socks5 1.2.3.4 1080")

        assertEquals(
            PluginConfiguration.Proxy(type = PluginConfiguration.Proxy.Type.SOCKS5, host = "1.2.3.4", port = 1080),
            fixture.savedConfig.tgConfig.proxy
        )
        assertEquals(listOf(plain(setup.saved.proxy("SOCKS5 1.2.3.4:1080"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_proxy_password_from_player_without_unsafe_WHEN_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user hunter2", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals(null, fixture.savedConfig.tgConfig.proxy)
    }

    @Test
    fun GIVEN_proxy_password_from_player_with_unsafe_WHEN_sets_proxy_THEN_it_is_saved_and_not_shown() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user hunter2 --unsafe", fixture.admin)

        val proxy = fixture.savedConfig.tgConfig.proxy
        assertEquals("user" to "hunter2", proxy?.credentials)
        assertTrue(fixture.repliesOf(fixture.admin).none { reply -> "hunter2" in reply })
    }

    @Test
    fun GIVEN_proxy_username_without_password_from_player_WHEN_sets_proxy_THEN_it_is_saved() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080 user", fixture.admin)

        assertEquals("user", fixture.savedConfig.tgConfig.proxy?.username)
    }

    @Test
    fun GIVEN_port_out_of_range_WHEN_console_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy http 1.2.3.4 70000")

        assertEquals(listOf(plain(setup.invalidPort)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_unknown_proxy_type_WHEN_console_sets_proxy_THEN_is_refused() {
        fixture.execute("mb telegram proxy ftp 1.2.3.4 21")

        assertEquals(listOf(plain(setup.invalidProxyType)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_proxy_WHEN_console_turns_it_off_THEN_it_is_removed() {
        fixture.execute("mb telegram proxy http 1.2.3.4 8080")

        fixture.execute("mb telegram proxy off")

        assertEquals(null, fixture.savedConfig.tgConfig.proxy)
        assertEquals(plain(setup.saved.proxyRemoved), fixture.consoleReplies.last())
    }

    @Test
    fun GIVEN_api_url_WHEN_console_sets_it_and_then_default_THEN_it_is_saved_and_cleared() {
        fixture.execute("mb telegram api-url http://localhost:8081/")
        assertEquals("http://localhost:8081", fixture.savedConfig.tgConfig.apiUrl)

        fixture.execute("mb telegram api-url default")

        assertEquals("", fixture.savedConfig.tgConfig.apiUrl)
    }

    @Test
    fun GIVEN_api_url_without_scheme_WHEN_console_sets_it_THEN_it_is_saved_with_https() {
        fixture.execute("mb telegram api-url tg.example.com:8443")

        assertEquals("https://tg.example.com:8443", fixture.savedConfig.tgConfig.apiUrl)
    }

    @Test
    fun GIVEN_api_url_with_a_path_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram api-url https://tg.example.com/bot")

        assertEquals(listOf(plain(setup.invalidUrl)), fixture.consoleReplies)
        assertEquals("", fixture.savedConfig.tgConfig.apiUrl)
    }

    @Test
    fun GIVEN_api_url_with_a_query_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram api-url https://tg.example.com/?token=1")

        assertEquals(listOf(plain(setup.invalidUrl)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_api_url_of_another_scheme_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram api-url ftp://tg.example.com")

        assertEquals(listOf(plain(setup.invalidUrl)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_connected_bot_WHEN_console_asks_for_bind_code_THEN_reads_how_to_send_it_and_then_the_result() {
        fixture.telegramSetup.status.value = MessengerStatus.Connected("@ServerBot")
        val bound = LocalizedText.shared("Chat -1001234567890 is bound")

        fixture.execute("mb telegram bind")
        fixture.telegramSetup.bindCallbacks.single().invoke(bound)

        assertEquals(
            listOf(plain(setup.telegramBindIssued("/bind@ServerBot 48213705", 10)), plain(bound)),
            fixture.consoleReplies
        )
        assertTrue("/bind@ServerBot 48213705" in fixture.consoleReplies.first())
    }

    @Test
    fun GIVEN_bot_that_is_still_connecting_WHEN_console_asks_for_bind_code_THEN_reads_bind_without_bot_name() {
        fixture.telegramSetup.status.value = MessengerStatus.Connecting

        fixture.execute("mb telegram bind")

        assertTrue("Send /bind 48213705 to the Telegram chat" in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_bot_without_token_WHEN_console_asks_for_bind_code_THEN_reads_how_to_set_the_token() {
        fixture.execute("mb telegram bind")

        assertEquals(listOf(plain(setup.status.disabled("Telegram", "telegram"))), fixture.consoleReplies)
        assertTrue(fixture.telegramSetup.bindCallbacks.isEmpty())
    }

    @Test
    fun GIVEN_checks_of_the_bot_WHEN_console_runs_check_THEN_reads_each_with_its_mark() {
        fixture.telegramSetup.checks = listOf(
            DiagnosticCheck(DiagnosticCheck.Level.OK, LocalizedText.shared("Token is valid")),
            DiagnosticCheck(DiagnosticCheck.Level.WARNING, LocalizedText.shared("Topic is not set")),
            DiagnosticCheck(DiagnosticCheck.Level.ERROR, LocalizedText.shared("Bot is not an admin"))
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
        assertEquals(
            TextColor.fromHexString("#db2c18"),
            fixture.console.messages.last().toComponent(Locale.ROOT).colorOf("Bot is not an admin")
        )
    }

    @Test
    fun GIVEN_bot_without_token_WHEN_console_runs_telegram_THEN_reads_guide_and_status() {
        fixture.execute("mb telegram")

        assertEquals(
            listOf(
                plain(fixture.translation.telegram.guide),
                "Telegram: not configured — /mb telegram token <token>",
                plain(setup.status.noChat),
                plain(setup.status.noProxy)
            ),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_connected_bot_WHEN_console_runs_telegram_THEN_reads_status_without_guide() {
        fixture.telegramSetup.status.value = MessengerStatus.Connected("@ServerBot")

        fixture.execute("mb telegram")

        assertEquals(plain(setup.status.connected("Telegram", "@ServerBot")), fixture.consoleReplies.first())
    }
}
