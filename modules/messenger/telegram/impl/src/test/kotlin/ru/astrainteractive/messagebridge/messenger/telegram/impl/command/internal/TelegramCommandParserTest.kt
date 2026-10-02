@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.command.internal

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.messagebridge.messenger.telegram.impl.command.model.TelegramCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelegramCommandParserTest {
    private val mapper = TelegramCommandParser(botUserName = { BOT_USER_NAME })

    @Test
    fun GIVEN_vanilla_WHEN_mapped_THEN_it_is_vanilla() = runTest {
        assertEquals(TelegramCommand.Vanilla, mapper.map("/vanilla"))
    }

    @Test
    fun GIVEN_vanilla_with_bot_name_WHEN_mapped_THEN_it_is_vanilla() = runTest {
        assertEquals(TelegramCommand.Vanilla, mapper.map("/vanilla@$BOT_USER_NAME"))
    }

    @Test
    fun GIVEN_bot_name_in_another_case_WHEN_mapped_THEN_it_is_for_this_bot() = runTest {
        assertEquals(TelegramCommand.Vanilla, mapper.map("/vanilla@mybridgebot"))
    }

    @Test
    fun GIVEN_command_for_another_bot_WHEN_mapped_THEN_it_is_ignored() = runTest {
        assertNull(mapper.map("/vanilla@OtherBot"))
    }

    @Test
    fun GIVEN_longer_command_WHEN_mapped_THEN_it_is_not_a_prefix_match() = runTest {
        assertNull(mapper.map("/linking 1234"))
    }

    @Test
    fun GIVEN_plain_text_WHEN_mapped_THEN_it_is_not_a_command() = runTest {
        assertNull(mapper.map("hello /vanilla"))
    }

    private companion object {
        const val BOT_USER_NAME = "MyBridgeBot"
    }
}
