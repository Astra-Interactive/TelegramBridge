@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramBotCommandTest {
    private fun parse(text: String): TelegramBotCommand? = TelegramBotCommand.parse(text, BOT_USER_NAME)

    @Test
    fun GIVEN_command_with_argument_WHEN_parsed_THEN_name_and_argument_are_split() {
        assertEquals(TelegramBotCommand(name = "/link", argument = "1234"), parse("/link 1234"))
    }

    @Test
    fun GIVEN_command_with_bot_name_WHEN_parsed_THEN_name_has_no_mention() {
        assertEquals(TelegramBotCommand(name = "/vanilla", argument = ""), parse("/vanilla@$BOT_USER_NAME"))
    }

    @Test
    fun GIVEN_bot_name_in_another_case_WHEN_parsed_THEN_it_is_for_this_bot() {
        assertEquals(TelegramBotCommand(name = "/vanilla", argument = ""), parse("/vanilla@mybridgebot"))
    }

    @Test
    fun GIVEN_command_for_another_bot_WHEN_parsed_THEN_it_is_ignored() {
        assertNull(parse("/vanilla@OtherBot"))
    }

    @Test
    fun GIVEN_unknown_bot_name_WHEN_command_has_a_mention_THEN_it_is_accepted() {
        val command = TelegramBotCommand.parse("/bind@AnyBot 12345678", botUserName = null)

        assertEquals(TelegramBotCommand(name = "/bind", argument = "12345678"), command)
    }

    @Test
    fun GIVEN_extra_spaces_WHEN_parsed_THEN_argument_is_trimmed() {
        assertEquals(TelegramBotCommand(name = "/bind", argument = "12345678"), parse("  /bind   12345678 "))
    }

    private companion object {
        const val BOT_USER_NAME = "MyBridgeBot"
    }
}
