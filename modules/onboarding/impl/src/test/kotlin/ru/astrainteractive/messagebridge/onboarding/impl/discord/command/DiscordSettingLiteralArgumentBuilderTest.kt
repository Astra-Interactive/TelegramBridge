@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.impl.discord.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.impl.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordSettingLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val setup = fixture.onboardingTranslation.setup
    private val token = "MTIzNDU2Nzg5MDEyMzQ1Njc4" + ".GAbCdE." + "Xx0123456789abcdefghijklmnopqrstuvwxyzAB"

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    @Test
    fun GIVEN_player_without_unsafe_WHEN_sets_discord_token_THEN_is_refused() {
        fixture.execute("mb discord token $token", fixture.admin)

        assertEquals(listOf(plain(setup.unsafeRequired)), fixture.repliesOf(fixture.admin))
        assertEquals("", fixture.savedConfig.jdaConfig.token)
    }

    @Test
    fun GIVEN_player_with_unsafe_WHEN_sets_discord_token_THEN_it_is_saved() {
        fixture.execute("mb discord token $token --unsafe", fixture.admin)

        assertEquals(token, fixture.savedConfig.jdaConfig.token)
    }

    @Test
    fun GIVEN_console_WHEN_sets_discord_token_THEN_reads_the_masked_token_and_never_the_token() {
        fixture.execute("mb discord token $token")

        assertEquals(token, fixture.savedConfig.jdaConfig.token)
        assertEquals(listOf(plain(setup.saved.token("MTIz…yzAB"))), fixture.consoleReplies)
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
        assertEquals(listOf(plain(setup.saved.channel("123456789012345678"))), fixture.consoleReplies)
    }

    @Test
    fun GIVEN_channel_name_or_id_of_a_wrong_length_WHEN_console_sets_it_THEN_is_refused() {
        fixture.execute("mb discord channel general")
        fixture.execute("mb discord channel 1234567890123456")
        fixture.execute("mb discord channel 123456789012345678901")

        assertEquals(List(size = 3) { _ -> plain(setup.invalidChannel) }, fixture.consoleReplies)
        assertEquals("", fixture.savedConfig.jdaConfig.channelId)
    }

    @Test
    fun GIVEN_activity_with_spaces_WHEN_console_sets_it_THEN_whole_text_is_saved_without_the_edge_spaces() {
        fixture.execute("mb discord activity Playing on play.example.com ")

        assertEquals("Playing on play.example.com", fixture.savedConfig.jdaConfig.activity)
        assertEquals(listOf(plain(setup.saved.activity("Playing on play.example.com"))), fixture.consoleReplies)
    }
}
