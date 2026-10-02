@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.api.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.locale.MinecraftLocales
import ru.astrainteractive.klibs.mikro.core.util.tryCast
import java.util.Locale
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

    private fun clickEventsOf(text: LocalizableComponent): List<ClickEvent> {
        return text.toComponent(MinecraftLocales.EN_US)
            .selfAndDescendants()
            .mapNotNull { node -> node.clickEvent() }
    }

    private fun plainText(text: LocalizableComponent): String {
        return PlainTextComponentSerializer.plainText().serialize(text.toComponent(MinecraftLocales.EN_US))
    }

    private fun hoverTextOf(text: LocalizableComponent, locale: Locale = MinecraftLocales.EN_US): String {
        val hoverText = text.toComponent(locale)
            .selfAndDescendants()
            .firstNotNullOf { node -> node.hoverEvent()?.value()?.tryCast<Component>() }
        return PlainTextComponentSerializer.plainText().serialize(hoverText)
    }

    private fun reply(replyPlayerName: String = "Steve", replyMessage: String = "hello"): LocalizableComponent {
        return translation.chat.toMinecraftReply(
            playerName = "Alex",
            message = "hi",
            from = "TG",
            replyPlayerName = replyPlayerName,
            replyMessage = replyMessage
        )
    }

    @Test
    fun GIVEN_message_with_click_tag_WHEN_formatted_for_minecraft_THEN_message_has_no_click() {
        val message = translation.chat.toMinecraft(playerName = "user", message = clickText, from = "TG")

        assertEquals(emptyList(), clickEventsOf(message))
    }

    @Test
    fun GIVEN_author_with_click_tag_WHEN_formatted_for_minecraft_THEN_message_has_no_click() {
        val message = translation.chat.toMinecraft(playerName = clickText, message = "привет", from = "DS")

        assertEquals(emptyList(), clickEventsOf(message))
    }

    @Test
    fun GIVEN_message_with_color_tag_WHEN_formatted_for_minecraft_THEN_tag_is_shown_as_text() {
        val message = translation.chat.toMinecraft(playerName = "user", message = "<red>привет", from = "TG")

        assertTrue("<red>привет" in plainText(message), plainText(message))
    }

    @Test
    fun GIVEN_author_named_like_placeholder_WHEN_formatted_for_telegram_THEN_name_is_not_replaced_again() {
        val message = translation.chat.toTelegram(playerName = "%message%", message = "hi", from = "MC")

        assertEquals("[MC] %message%:\nhi", plainText(message))
    }

    @Test
    fun GIVEN_death_without_cause_WHEN_rendered_THEN_unknown_cause_is_in_the_same_language() {
        val message = translation.player.died(name = "Steve", cause = null)

        assertEquals("Player Steve died: unknown cause", plainText(message))
    }

    @Test
    fun GIVEN_death_with_cause_WHEN_rendered_THEN_cause_is_shown_as_text() {
        val message = translation.player.died(name = "Steve", cause = "<red>lava")

        assertEquals("Player Steve died: <red>lava", plainText(message))
    }

    @Test
    fun GIVEN_reply_WHEN_formatted_for_minecraft_THEN_line_names_replied_player() {
        assertEquals("[TG] Alex ↪ Steve: hi", plainText(reply()))
    }

    @Test
    fun GIVEN_reply_WHEN_hovered_THEN_replied_message_is_shown() {
        assertEquals("Reply to Steve:\nhello", hoverTextOf(reply()))
    }

    @Test
    fun GIVEN_replied_message_with_click_tag_WHEN_hovered_THEN_tag_is_shown_as_text() {
        val message = reply(replyMessage = clickText)

        assertEquals("Reply to Steve:\n$clickText", hoverTextOf(message))
        assertEquals(emptyList(), clickEventsOf(message))
    }

    @Test
    fun GIVEN_replied_player_with_click_tag_WHEN_formatted_for_minecraft_THEN_message_has_no_click() {
        val message = reply(replyPlayerName = clickText)

        assertEquals(emptyList(), clickEventsOf(message))
    }

    @Test
    fun GIVEN_replied_message_of_max_length_WHEN_hovered_THEN_it_is_shown_whole() {
        val repliedMessage = "a".repeat(200)

        assertEquals("Reply to Steve:\n$repliedMessage", hoverTextOf(reply(replyMessage = repliedMessage)))
    }

    @Test
    fun GIVEN_replied_message_over_max_length_WHEN_hovered_THEN_it_is_cut() {
        val message = reply(replyMessage = "a".repeat(201))

        assertEquals("Reply to Steve:\n${"a".repeat(200)}…", hoverTextOf(message))
    }

    @Test
    fun GIVEN_cut_falls_inside_emoji_WHEN_hovered_THEN_emoji_is_dropped_whole() {
        val message = reply(replyMessage = "a".repeat(199) + "😀" + "b")

        assertEquals("Reply to Steve:\n${"a".repeat(199)}…", hoverTextOf(message))
    }

    @Test
    fun GIVEN_reply_to_media_without_caption_WHEN_hovered_THEN_media_is_named_in_reader_language() {
        val message = reply(replyMessage = "")

        assertEquals("Reply to Steve:\n[media]", hoverTextOf(message))
        assertEquals("Ответ на сообщение Steve:\n[медиа]", hoverTextOf(message, MinecraftLocales.RU_RU))
    }
}
