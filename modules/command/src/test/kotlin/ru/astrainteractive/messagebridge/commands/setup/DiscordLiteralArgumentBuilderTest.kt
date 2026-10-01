@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.commands.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordLiteralArgumentBuilderTest {
    private val fixture = SetupCommandFixture()
    private val setup = fixture.translation.setup
    private val token = "MTIzNDU2Nzg5MDEyMzQ1Njc4" + ".GAbCdE." + "Xx0123456789abcdefghijklmnopqrstuvwxyzAB"

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun Component.selfAndDescendants(): List<Component> {
        return listOf(this) + children().flatMap { child -> child.selfAndDescendants() }
    }

    @Test
    fun GIVEN_socks5_proxy_WHEN_console_sets_it_for_discord_THEN_is_refused() {
        fixture.execute("mb discord proxy socks5 1.2.3.4 1080")

        assertEquals(listOf(plain(setup.socksNotSupported)), fixture.consoleReplies)
        assertEquals(null, fixture.savedConfig.jdaConfig.proxy)
    }

    @Test
    fun GIVEN_http_proxy_WHEN_console_sets_it_for_discord_THEN_it_is_saved() {
        fixture.execute("mb discord proxy http 1.2.3.4 8080")

        assertEquals(
            PluginConfiguration.Proxy(type = PluginConfiguration.Proxy.Type.HTTP, host = "1.2.3.4", port = 8080),
            fixture.savedConfig.jdaConfig.proxy
        )
    }

    @Test
    fun GIVEN_player_without_unsafe_WHEN_sets_discord_token_THEN_is_refused() {
        fixture.execute("mb discord token $token", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals("", fixture.savedConfig.jdaConfig.token)
    }

    @Test
    fun GIVEN_console_WHEN_sets_discord_token_and_bot_connects_THEN_reads_bot_name_and_never_the_token() {
        fixture.execute("mb discord token $token")
        fixture.discordSetup.status.value = MessengerStatus.Connected("ServerBot#0001")

        assertEquals(token, fixture.savedConfig.jdaConfig.token)
        assertEquals(
            listOf(plain(setup.saved.token("MTIz…yzAB")), plain(setup.saved.connected("ServerBot#0001"))),
            fixture.consoleReplies
        )
        assertTrue(fixture.consoleReplies.none { reply -> token in reply })
    }

    @Test
    fun GIVEN_client_secret_instead_of_token_WHEN_console_sets_it_THEN_reads_invalid_token() {
        fixture.execute("mb discord token 0123456789abcdef0123456789abcdef")

        assertEquals(listOf(plain(setup.invalidDiscordToken)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_channel_id_WHEN_console_sets_it_THEN_it_is_saved() {
        fixture.execute("mb discord channel 123456789012345678")

        assertEquals("123456789012345678", fixture.savedConfig.jdaConfig.channelId)
    }

    @Test
    fun GIVEN_channel_name_instead_of_id_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb discord channel general")

        assertEquals(listOf(plain(setup.invalidChannel)), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_activity_with_spaces_WHEN_console_sets_it_THEN_whole_text_is_saved() {
        fixture.execute("mb discord activity Playing on play.example.com")

        assertEquals("Playing on play.example.com", fixture.savedConfig.jdaConfig.activity)
    }

    @Test
    fun GIVEN_connected_bot_WHEN_console_asks_for_bind_code_THEN_reads_the_discord_command() {
        fixture.discordSetup.status.value = MessengerStatus.Connected("ServerBot#0001")

        fixture.execute("mb discord bind")

        assertEquals(listOf(plain(setup.discordBindIssued("48213705", 10))), fixture.consoleReplies)
        assertTrue("!bind 48213705" in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_connected_bot_WHEN_console_asks_for_invite_THEN_reads_a_clickable_link() {
        val url = "https://discord.com/oauth2/authorize?client_id=42&scope=bot"
        fixture.discordSetup.inviteUrl = url

        fixture.execute("mb discord invite")

        val clickEvents = fixture.console.messages.single()
            .toComponent(Locale.ROOT)
            .selfAndDescendants()
            .mapNotNull(Component::clickEvent)
        assertEquals(listOf(ClickEvent.openUrl(url)), clickEvents)
        assertTrue(url in fixture.consoleReplies.single())
    }

    @Test
    fun GIVEN_bot_that_is_not_connected_WHEN_console_asks_for_invite_THEN_reads_invite_unavailable_and_why() {
        fixture.execute("mb discord invite")

        assertEquals(
            listOf(plain(setup.inviteUnavailable), plain(setup.status.disabled("Discord", "discord"))),
            fixture.consoleReplies
        )
    }
}
