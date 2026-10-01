@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.OnboardingFixture
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramSettingLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.translation.setup
    private val token = "123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw1"
    private val maskedToken = "1234…saw1"

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    @Test
    fun GIVEN_player_without_unsafe_WHEN_sets_token_THEN_is_refused_and_nothing_is_saved() {
        fixture.execute("mb telegram token $token", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals("", fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_player_with_unsafe_before_the_token_WHEN_sets_token_THEN_is_refused() {
        fixture.execute("mb telegram token --unsafe $token", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals("", fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_player_with_unsafe_WHEN_sets_token_THEN_token_is_saved_without_the_flag_and_is_masked() {
        fixture.execute("mb telegram token $token --unsafe", fixture.admin)

        assertEquals(token, fixture.savedConfig.tgConfig.token)
        assertEquals(listOf(plain(setup.saved.token(maskedToken))), fixture.repliesOf(fixture.admin))
    }

    @Test
    fun GIVEN_console_WHEN_sets_token_and_bot_connects_THEN_reads_bot_name_and_never_the_token() {
        fixture.execute("mb telegram token $token")
        fixture.telegram.status.value = MessengerStatus.Connecting
        fixture.telegram.status.value = MessengerStatus.Connected("@ServerBot")

        assertEquals(token, fixture.savedConfig.tgConfig.token)
        assertEquals(
            listOf(plain(setup.saved.token(maskedToken)), plain(setup.saved.connected("@ServerBot"))),
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
    fun GIVEN_value_that_is_not_a_token_WHEN_console_sets_it_THEN_reads_invalid_token() {
        fixture.execute("mb telegram token 123456")

        assertEquals(listOf(plain(setup.invalidTelegramToken)), fixture.consoleReplies)
        assertEquals("", fixture.savedConfig.tgConfig.token)
    }

    @Test
    fun GIVEN_two_tokens_WHEN_console_sets_them_at_once_THEN_reads_invalid_token() {
        fixture.execute("mb telegram token $token $token")

        assertEquals(listOf(plain(setup.invalidTelegramToken)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_sets_chat_THEN_reads_no_permission() {
        fixture.execute("mb telegram chat -1001234567890", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
        assertEquals("", fixture.savedConfig.tgConfig.chatID)
    }

    @Test
    fun GIVEN_chat_id_WHEN_console_sets_it_THEN_it_is_saved_without_waiting_for_the_bot() {
        fixture.editConfig { config -> config.copy(tgConfig = config.tgConfig.copy(token = token)) }

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
    fun GIVEN_zero_chat_id_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram chat 0")

        assertEquals(listOf(plain(setup.invalidChat)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_config_with_an_error_WHEN_console_sets_chat_THEN_nothing_is_saved_and_file_is_left_as_is() {
        val brokenConfig = "tgConfig:\n  max_telegram_message_length: not-a-number\n"
        fixture.configFile.writeText(brokenConfig)

        fixture.execute("mb telegram chat -1001234567890")

        val reply = fixture.consoleReplies.single()
        assertTrue(reply.startsWith("config.yml has an error, so nothing is saved"), reply)
        assertTrue("line 2" in reply, reply)
        assertEquals(brokenConfig, fixture.configFile.readText())
    }

    @Test
    fun GIVEN_topic_WHEN_console_sets_it_and_then_none_THEN_topic_is_saved_and_cleared() {
        fixture.execute("mb telegram topic 12")
        assertEquals("12", fixture.savedConfig.tgConfig.topicID)

        fixture.execute("mb telegram topic NONE")

        assertEquals("", fixture.savedConfig.tgConfig.topicID)
        assertEquals(listOf(plain(setup.saved.topic("12")), plain(setup.saved.topicRemoved)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_topic_that_is_not_a_positive_number_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram topic general")
        fixture.execute("mb telegram topic 0")

        assertEquals(listOf(plain(setup.invalidTopic), plain(setup.invalidTopic)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_api_url_WHEN_console_sets_it_and_then_default_THEN_it_is_saved_and_cleared() {
        fixture.execute("mb telegram api-url http://localhost:8081/")
        assertEquals("http://localhost:8081", fixture.savedConfig.tgConfig.apiUrl)

        fixture.execute("mb telegram api-url Default")

        assertEquals("", fixture.savedConfig.tgConfig.apiUrl)
        assertEquals(
            listOf(plain(setup.saved.apiUrl("http://localhost:8081")), plain(setup.saved.apiUrlRemoved)),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_api_url_without_scheme_WHEN_console_sets_it_THEN_it_is_saved_with_https() {
        fixture.execute("mb telegram api-url tg.example.com:8443")

        assertEquals("https://tg.example.com:8443", fixture.savedConfig.tgConfig.apiUrl)
    }

    @Test
    fun GIVEN_api_url_with_credentials_WHEN_console_sets_it_THEN_they_are_saved_and_not_shown() {
        fixture.execute("mb telegram api-url https://user:secret@tg.example.com")

        assertEquals("https://user:secret@tg.example.com", fixture.savedConfig.tgConfig.apiUrl)
        assertEquals(listOf(plain(setup.saved.apiUrl("https://tg.example.com"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_api_url_with_a_path_a_query_or_a_fragment_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram api-url https://tg.example.com/bot")
        fixture.execute("mb telegram api-url https://tg.example.com/?token=1")
        fixture.execute("mb telegram api-url https://tg.example.com/#top")

        assertEquals(List(size = 3) { _ -> plain(setup.invalidUrl) }, fixture.consoleReplies)
        assertEquals("", fixture.savedConfig.tgConfig.apiUrl)
    }

    @Test
    fun GIVEN_api_url_of_another_scheme_or_without_host_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram api-url ftp://tg.example.com")
        fixture.execute("mb telegram api-url https://")

        assertEquals(List(size = 2) { _ -> plain(setup.invalidUrl) }, fixture.consoleReplies)
    }

    @Test
    fun GIVEN_api_url_that_is_not_an_address_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb telegram api-url http://tg example com")

        assertEquals(listOf(plain(setup.invalidUrl)), fixture.consoleReplies)
    }
}
