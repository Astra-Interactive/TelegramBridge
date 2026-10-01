@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.discord.command

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscordBindCommandParserTest {
    private val parser = DiscordBindCommandParser()

    @Test
    fun GIVEN_bind_with_code_WHEN_mapped_THEN_code_is_returned() {
        assertEquals("12345678", parser.map("  !bind   12345678 "))
    }

    @Test
    fun GIVEN_bind_without_code_WHEN_mapped_THEN_code_is_empty() {
        assertEquals("", parser.map("!bind"))
    }

    @Test
    fun GIVEN_other_message_WHEN_mapped_THEN_it_is_not_bind() {
        assertNull(parser.map("!binder 12345678"))
        assertNull(parser.map("hello !bind 12345678"))
    }
}
