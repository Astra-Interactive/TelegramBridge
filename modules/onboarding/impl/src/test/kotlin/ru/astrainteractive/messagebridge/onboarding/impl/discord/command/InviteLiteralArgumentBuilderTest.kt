@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.impl.discord.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.impl.fake.OnboardingFixture
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InviteLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.onboardingTranslation.setup

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
    fun GIVEN_bot_that_is_not_connected_WHEN_console_asks_for_invite_THEN_reads_invite_unavailable() {
        fixture.execute("mb discord invite")

        assertEquals(listOf(plain(setup.inviteUnavailable)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_player_without_permission_WHEN_asks_for_invite_THEN_reads_no_permission() {
        fixture.discord.inviteUrl = "https://discord.com/oauth2/authorize?client_id=42&scope=bot"

        fixture.execute("mb discord invite", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
