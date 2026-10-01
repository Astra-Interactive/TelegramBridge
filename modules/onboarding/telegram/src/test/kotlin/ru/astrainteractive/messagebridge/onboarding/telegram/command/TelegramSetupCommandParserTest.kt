@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.telegram.command

import ru.astrainteractive.messagebridge.onboarding.telegram.model.TelegramSetupCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramSetupCommandParserTest {
    private val parser = TelegramSetupCommandParser(botUserName = { BOT_USER_NAME })

    @Test
    fun GIVEN_bind_with_code_WHEN_parsed_THEN_code_is_kept_as_text() {
        assertEquals(TelegramSetupCommand.Bind("01234567"), parser.map("/bind@$BOT_USER_NAME 01234567"))
    }

    @Test
    fun GIVEN_bind_without_code_WHEN_parsed_THEN_code_is_empty() {
        assertEquals(TelegramSetupCommand.Bind(""), parser.map("/bind"))
    }

    @Test
    fun GIVEN_minfo_WHEN_parsed_THEN_it_is_chat_info() {
        assertEquals(TelegramSetupCommand.ChatInfo, parser.map("/minfo"))
    }

    @Test
    fun GIVEN_minfo_for_another_bot_WHEN_parsed_THEN_it_is_ignored() {
        assertNull(parser.map("/minfo@OtherBot"))
    }

    @Test
    fun GIVEN_command_of_the_relay_WHEN_parsed_THEN_it_is_not_a_setup_command() {
        assertNull(parser.map("/vanilla"))
    }

    private companion object {
        const val BOT_USER_NAME = "MyBridgeBot"
    }
}
