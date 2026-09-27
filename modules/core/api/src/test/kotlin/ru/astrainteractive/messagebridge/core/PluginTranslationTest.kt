@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginTranslationTest {

    private val translation = PluginTranslation()

    /** Text a Telegram or Discord user can send: if it were parsed, every player would get a clickable command. */
    private val clickText = "<click:run_command:'/op thief'>жми</click>"

    private fun Component.selfAndDescendants(): List<Component> {
        return listOf(this) + children().flatMap { child -> child.selfAndDescendants() }
    }

    private fun clickEventsOf(text: LocalizableComponent): List<ClickEvent<*>> {
        return text.toComponent(MinecraftLocales.EN_US)
            .selfAndDescendants()
            .mapNotNull { node -> node.clickEvent() }
    }

    private fun plainText(text: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(text.toComponent(MinecraftLocales.EN_US))
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
    fun GIVEN_author_named_like_placeholder_WHEN_formatted_for_telegram_THEN_name_is_not_replaced_again() {
        val message = translation.telegramMessageFormat(playerName = "%message%", message = "hi", from = "MC")

        assertEquals("[MC] %message%:\nhi", plainText(message))
    }

    @Test
    fun GIVEN_code_created_message_WHEN_rendered_THEN_code_hint_is_shown_as_text() {
        val message = translation.link.codeCreated(code = 1234)

        assertTrue("/link <code>" in plainText(message), plainText(message))
    }

    @Test
    fun GIVEN_death_without_cause_WHEN_rendered_THEN_unknown_cause_is_in_the_same_language() {
        val message = translation.playerDiedMessage(name = "Steve", cause = null)

        assertEquals("Player Steve died: unknown cause", plainText(message))
    }

    @Test
    fun GIVEN_death_with_cause_WHEN_rendered_THEN_cause_is_shown_as_text() {
        val message = translation.playerDiedMessage(name = "Steve", cause = "<red>lava")

        assertEquals("Player Steve died: <red>lava", plainText(message))
    }
}
