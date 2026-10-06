@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.api.util

import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtTest {
    @Test
    fun GIVEN_text_of_max_length_WHEN_ellipsized_THEN_it_is_kept_whole() {
        assertEquals("abcde", "abcde".ellipsize(maxLength = 5))
    }

    @Test
    fun GIVEN_text_over_max_length_WHEN_ellipsized_THEN_it_is_cut_and_ends_with_ellipsis() {
        assertEquals("abcde…", "abcdefgh".ellipsize(maxLength = 5))
    }

    @Test
    fun GIVEN_cut_that_falls_inside_emoji_WHEN_ellipsized_THEN_emoji_is_dropped_whole() {
        assertEquals("abcd…", ("abcd" + "😀" + "e").ellipsize(maxLength = 5))
    }

    @Test
    fun GIVEN_space_before_the_cut_WHEN_ellipsized_THEN_ellipsis_follows_the_last_word() {
        assertEquals("ab…", "ab  cdef".ellipsize(maxLength = 4))
    }
}
