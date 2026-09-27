@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.kyori.AutoComponentSerializer
import ru.astrainteractive.astralibs.string.StringDesc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginTranslationTest {

    private val translation = PluginTranslation()

    /** Text a Telegram or Discord user can send: without escaping, every player would get a clickable command. */
    private val clickText = "<click:run_command:'/op thief'>жми</click>"

    private fun Component.selfAndDescendants(): List<Component> {
        return listOf(this) + children().flatMap { child -> child.selfAndDescendants() }
    }

    private fun clickEventsOf(stringDesc: StringDesc): List<ClickEvent<*>> {
        return AutoComponentSerializer.toComponent(stringDesc)
            .selfAndDescendants()
            .mapNotNull { node -> node.clickEvent() }
    }

    private fun plainText(stringDesc: StringDesc): String {
        return PlainTextComponentSerializer.plainText().serialize(AutoComponentSerializer.toComponent(stringDesc))
    }

    @Test
    fun GIVEN_message_with_click_tag_WHEN_formatted_for_minecraft_THEN_message_has_no_click() {
        val message = translation.minecraftMessageFormat(playerName = "user", message = clickText, from = "TG")

        assertEquals(emptyList(), clickEventsOf(message))
    }

    @Test
    fun GIVEN_author_with_click_tag_WHEN_formatted_for_minecraft_THEN_message_has_no_click() {
        val message = translation.minecraftMessageFormat(playerName = clickText, message = "привет", from = "DS")

        assertEquals(emptyList(), clickEventsOf(message))
    }

    @Test
    fun GIVEN_message_with_color_tag_WHEN_formatted_for_minecraft_THEN_tag_is_shown_as_text() {
        val message = translation.minecraftMessageFormat(playerName = "user", message = "<red>привет", from = "TG")

        assertTrue("<red>привет" in plainText(message), plainText(message))
    }

    @Test
    fun GIVEN_code_created_message_WHEN_rendered_THEN_code_hint_is_shown_as_text() {
        val message = translation.link.codeCreated(code = 1234)

        assertTrue("/link <code>" in plainText(message), plainText(message))
    }
}
