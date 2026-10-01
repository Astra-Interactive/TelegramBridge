@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.secret.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecretInputTest {
    @Test
    fun GIVEN_words_split_by_several_spaces_WHEN_parsed_THEN_reads_each_word_once() {
        val input = SecretInput.parse("  user   hunter2 ")

        assertEquals(listOf("user", "hunter2"), input.words)
        assertFalse(input.isUnsafe)
    }

    @Test
    fun GIVEN_unsafe_flag_at_the_end_WHEN_parsed_THEN_flag_is_taken_off() {
        val input = SecretInput.parse("user hunter2 --unsafe")

        assertEquals(listOf("user", "hunter2"), input.words)
        assertTrue(input.isUnsafe)
    }

    @Test
    fun GIVEN_unsafe_flag_before_the_secret_WHEN_parsed_THEN_it_is_a_word_and_does_not_confirm() {
        val input = SecretInput.parse("--unsafe hunter2")

        assertEquals(listOf("--unsafe", "hunter2"), input.words)
        assertFalse(input.isUnsafe)
    }

    @Test
    fun GIVEN_only_the_unsafe_flag_WHEN_parsed_THEN_there_is_no_secret() {
        val input = SecretInput.parse("--unsafe")

        assertEquals(emptyList(), input.words)
        assertTrue(input.isUnsafe)
    }
}
