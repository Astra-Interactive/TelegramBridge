@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramCommandParserTest {
    private val mapper = TelegramCommandParser(botUserName = { BOT_USER_NAME })

    @Test
    fun GIVEN_vanilla_WHEN_mapped_THEN_it_is_vanilla() {
        assertEquals(TelegramCommand.Vanilla, mapper.map("/vanilla"))
    }

    @Test
    fun GIVEN_vanilla_with_bot_name_WHEN_mapped_THEN_it_is_vanilla() {
        assertEquals(TelegramCommand.Vanilla, mapper.map("/vanilla@$BOT_USER_NAME"))
    }

    @Test
    fun GIVEN_bot_name_in_another_case_WHEN_mapped_THEN_it_is_for_this_bot() {
        assertEquals(TelegramCommand.Vanilla, mapper.map("/vanilla@mybridgebot"))
    }

    @Test
    fun GIVEN_link_with_bot_name_and_code_WHEN_mapped_THEN_code_is_parsed() {
        assertEquals(TelegramCommand.Link(1234), mapper.map("/link@$BOT_USER_NAME 1234"))
    }

    @Test
    fun GIVEN_link_without_code_WHEN_mapped_THEN_code_is_invalid() {
        assertEquals(TelegramCommand.Link(-1), mapper.map("/link"))
    }

    @Test
    fun GIVEN_minfo_with_bot_name_WHEN_mapped_THEN_it_is_chat_info() {
        assertEquals(TelegramCommand.ChatInfo, mapper.map("/minfo@$BOT_USER_NAME"))
    }

    @Test
    fun GIVEN_bind_with_bot_name_and_code_WHEN_mapped_THEN_code_is_kept_as_text() {
        assertEquals(TelegramCommand.Bind("01234567"), mapper.map("/bind@$BOT_USER_NAME 01234567"))
    }

    @Test
    fun GIVEN_bind_with_extra_spaces_WHEN_mapped_THEN_code_is_trimmed() {
        assertEquals(TelegramCommand.Bind("12345678"), mapper.map("  /bind   12345678 "))
    }

    @Test
    fun GIVEN_bind_without_code_WHEN_mapped_THEN_code_is_empty() {
        assertEquals(TelegramCommand.Bind(""), mapper.map("/bind"))
    }

    @Test
    fun GIVEN_command_for_another_bot_WHEN_mapped_THEN_it_is_ignored() {
        assertNull(mapper.map("/vanilla@OtherBot"))
    }

    @Test
    fun GIVEN_unknown_bot_name_WHEN_command_has_a_mention_THEN_it_is_accepted() {
        val mapper = TelegramCommandParser(botUserName = { null })

        assertEquals(TelegramCommand.Bind("12345678"), mapper.map("/bind@AnyBot 12345678"))
    }

    @Test
    fun GIVEN_longer_command_WHEN_mapped_THEN_it_is_not_a_prefix_match() {
        assertNull(mapper.map("/linking 1234"))
    }

    @Test
    fun GIVEN_plain_text_WHEN_mapped_THEN_it_is_not_a_command() {
        assertNull(mapper.map("hello /vanilla"))
    }

    private companion object {
        const val BOT_USER_NAME = "MyBridgeBot"
    }
}
