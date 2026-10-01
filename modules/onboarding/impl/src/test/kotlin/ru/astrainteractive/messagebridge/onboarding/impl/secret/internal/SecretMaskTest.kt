@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.impl.secret.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class SecretMaskTest {
    @Test
    fun GIVEN_token_long_enough_WHEN_masked_THEN_shows_four_characters_at_each_end() {
        assertEquals("abcd…9012", "abcdefgh9012".masked())
    }

    @Test
    fun GIVEN_short_token_WHEN_masked_THEN_shows_nothing_of_it() {
        assertEquals("…", "abcdefg9012".masked())
    }

    @Test
    fun GIVEN_address_with_a_username_and_a_password_WHEN_shown_THEN_they_are_left_out() {
        assertEquals("https://tg.example.com:8443", "https://user:secret@tg.example.com:8443".withoutCredentials())
    }

    @Test
    fun GIVEN_address_without_credentials_WHEN_shown_THEN_it_is_left_as_is() {
        assertEquals("http://localhost:8081", "http://localhost:8081".withoutCredentials())
    }
}
