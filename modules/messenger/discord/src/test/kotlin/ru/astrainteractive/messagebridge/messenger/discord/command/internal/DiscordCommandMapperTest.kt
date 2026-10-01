@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.discord.command.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscordCommandMapperTest {
    private val mapper = DiscordCommandMapper()

    @Test
    fun GIVEN_bind_with_code_WHEN_mapped_THEN_code_is_returned() {
        assertEquals("12345678", mapper.mapBindCode("  !bind   12345678 "))
    }

    @Test
    fun GIVEN_bind_without_code_WHEN_mapped_THEN_code_is_empty() {
        assertEquals("", mapper.mapBindCode("!bind"))
    }

    @Test
    fun GIVEN_other_message_WHEN_mapped_THEN_it_is_not_bind() {
        assertNull(mapper.mapBindCode("!binder 12345678"))
        assertNull(mapper.mapBindCode("hello !bind 12345678"))
    }
}
