@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.command

import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.fake.OnboardingFixture
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReloadLiteralArgumentBuilderTest {
    private val fixture = OnboardingFixture()
    private val reload = fixture.translation.reload
    private val brokenConfig = "tgConfig:\n  max_telegram_message_length: not-a-number\n"

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun plain(message: LocalizableComponent): String = fixture.plainTextOf(message)

    private fun configWithChat(chatId: String): PluginConfiguration {
        return PluginConfiguration(tgConfig = PluginConfiguration.TelegramConfig(chatID = chatId))
    }

    @Test
    fun GIVEN_edited_config_WHEN_admin_reloads_THEN_reads_reload_completed_and_the_bots_run_with_the_edit() {
        fixture.writeConfig(configWithChat("-100123"))

        fixture.execute("mb reload", fixture.admin)

        assertEquals(listOf(plain(reload.started), plain(reload.completed)), fixture.repliesOf(fixture.admin))
        assertEquals("-100123", fixture.coreModule.config.value.tgConfig.chatID)
    }

    @Test
    fun GIVEN_config_with_an_error_WHEN_admin_reloads_THEN_reads_where_the_error_is_and_previous_settings_are_kept() {
        fixture.editConfig { config -> config.copy(tgConfig = config.tgConfig.copy(chatID = "-100123")) }
        fixture.configFile.writeText(brokenConfig)

        fixture.execute("mb reload", fixture.admin)

        val replies = fixture.repliesOf(fixture.admin)
        assertEquals(plain(reload.started), replies.first())
        assertTrue(replies.last().startsWith("config.yml has an error and is not applied"), replies.last())
        assertTrue("line 2" in replies.last(), replies.last())
        assertEquals(2, replies.size, "$replies")
        assertEquals("-100123", fixture.coreModule.config.value.tgConfig.chatID)
        assertEquals(brokenConfig, fixture.configFile.readText())
    }

    @Test
    fun GIVEN_config_that_is_fixed_after_an_error_WHEN_admin_reloads_again_THEN_the_bots_run_with_the_fixed_file() {
        fixture.configFile.writeText(brokenConfig)
        fixture.execute("mb reload", fixture.admin)
        fixture.writeConfig(configWithChat("-100456"))
        fixture.admin.messages.clear()

        fixture.execute("mb reload", fixture.admin)

        assertEquals(listOf(plain(reload.started), plain(reload.completed)), fixture.repliesOf(fixture.admin))
        assertEquals("-100456", fixture.coreModule.config.value.tgConfig.chatID)
    }

    @Test
    fun GIVEN_player_without_reload_permission_WHEN_reloads_THEN_reads_no_permission() {
        fixture.execute("mb reload", fixture.steve)

        assertEquals(listOf(plain(fixture.translation.commandError.noPermission)), fixture.repliesOf(fixture.steve))
    }
}
