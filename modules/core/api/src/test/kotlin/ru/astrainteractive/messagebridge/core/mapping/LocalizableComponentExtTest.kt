@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.mapping

import ru.astrainteractive.astralibs.localization.component.replace
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizableComponentExtTest {
    private val greeting = LocalizedText.build {
        translation(MinecraftLocales.EN_US, "&aHello")
        translation(MinecraftLocales.RU_RU, "&aПривет")
    }

    @Test
    fun GIVEN_translated_text_WHEN_to_messenger_text_THEN_first_language_is_used() {
        assertEquals("Hello", greeting.toMessengerText())
    }

    @Test
    fun GIVEN_text_for_every_language_WHEN_to_messenger_text_THEN_shared_text_is_used() {
        val text = LocalizedText.build {
            translation(MinecraftLocales.RU_RU, "Привет")
            shared("Hi")
        }

        assertEquals("Hi", text.toMessengerText())
    }

    @Test
    fun GIVEN_markup_WHEN_to_messenger_text_THEN_markup_is_dropped() {
        val text = LocalizedText.shared("<bold>Server</bold> &cstopped")

        assertEquals("Server stopped", text.toMessengerText())
    }

    @Test
    fun GIVEN_value_with_markup_WHEN_to_messenger_text_THEN_value_stays_literal() {
        val text = LocalizedText.shared("Online: %players%").replace("%players%", "<red>Steve")

        assertEquals("Online: <red>Steve", text.toMessengerText())
    }
}
