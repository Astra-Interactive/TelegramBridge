@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.api.text.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TextPreviewTest {
    @Test
    fun GIVEN_text_of_max_length_WHEN_previewed_THEN_it_is_kept_whole() {
        assertEquals("abcde", TextPreview.of("abcde", maxLength = 5).value)
    }

    @Test
    fun GIVEN_text_over_max_length_WHEN_previewed_THEN_it_is_cut_and_ends_with_ellipsis() {
        assertEquals("abcde…", TextPreview.of("abcdefgh", maxLength = 5).value)
    }

    @Test
    fun GIVEN_cut_that_falls_inside_emoji_WHEN_previewed_THEN_emoji_is_dropped_whole() {
        assertEquals("abcd…", TextPreview.of("abcd" + "😀" + "e", maxLength = 5).value)
    }

    @Test
    fun GIVEN_space_before_the_cut_WHEN_previewed_THEN_ellipsis_follows_the_last_word() {
        assertEquals("ab…", TextPreview.of("ab  cdef", maxLength = 4).value)
    }
}
