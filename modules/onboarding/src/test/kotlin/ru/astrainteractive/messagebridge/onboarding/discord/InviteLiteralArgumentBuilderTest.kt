@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.discord

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.messagebridge.onboarding.OnboardingFixture
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InviteLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.translation.setup

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun Component.selfAndDescendants(): List<Component> {
        return listOf(this) + children().flatMap { child -> child.selfAndDescendants() }
    }

    @Test
    fun GIVEN_connected_bot_WHEN_console_asks_for_invite_THEN_reads_a_clickable_link() {
        val url = "https://discord.com/oauth2/authorize?client_id=42&scope=bot"
        fixture.discord.inviteUrl = url

        fixture.execute("mb discord invite")

        val clickEvents = fixture.console.messages.single()
            .toComponent(Locale.ROOT)
            .selfAndDescendants()
            .mapNotNull(Component::clickEvent)
        assertEquals(listOf(ClickEvent.openUrl(url)), clickEvents)
        assertTrue(url in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_bot_without_token_WHEN_console_asks_for_invite_THEN_reads_invite_unavailable_and_why() {
        fixture.execute("mb discord invite")

        assertEquals(
            listOf(plain(setup.inviteUnavailable), "Discord: not configured — /mb discord token <token>"),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_bot_that_failed_WHEN_console_asks_for_invite_THEN_reads_invite_unavailable_and_the_reason() {
        fixture.discord.status.value = MessengerStatus.Failed(LocalizedText.shared("the token is revoked"))

        fixture.execute("mb discord invite")

        assertEquals(
            listOf(plain(setup.inviteUnavailable), "Discord: not connected — the token is revoked"),
            fixture.consoleReplies
        )
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_asks_for_invite_THEN_reads_no_permission() {
        fixture.discord.inviteUrl = "https://discord.com/oauth2/authorize?client_id=42&scope=bot"

        fixture.execute("mb discord invite", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
