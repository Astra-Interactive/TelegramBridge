@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.api.config

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import kotlin.test.Test
import kotlin.test.assertTrue

class LinkTranslationTest {
    private val translation = LinkTranslation()

    @Test
    fun GIVEN_code_created_message_WHEN_rendered_THEN_code_hint_is_shown_as_text() {
        val message = translation.link.codeCreated(code = 1234)

        val plainText = PlainTextComponentSerializer.plainText().serialize(message.toComponent(MinecraftLocales.EN_US))
        assertTrue("/link <code>" in plainText, plainText)
    }
}
