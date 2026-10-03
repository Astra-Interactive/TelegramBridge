@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.link.config

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import kotlin.test.Test
import kotlin.test.assertTrue

class LinkTranslationTest {
    private val translation = LinkTranslation()

    private fun plainText(text: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(text.toComponent(MinecraftLocales.EN_US))
    }

    @Test
    fun GIVEN_code_created_message_WHEN_rendered_THEN_code_hint_is_shown_as_text() {
        val message = translation.link.codeCreated(code = 1234)

        assertTrue("/link <code>" in plainText(message), plainText(message))
    }
}
