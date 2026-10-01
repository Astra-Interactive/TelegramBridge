@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.discord.internal

import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.fake.LogLine
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.api.model.DiscordFailure
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscordGuideLoggerTest {
    private val translation = OnboardingTranslation()
    private val logger = RecordingLogger()
    private val guideLogger = DiscordGuideLogger(translationKrate = FakeTranslationKrate(translation), logger = logger)

    @Test
    fun GIVEN_no_token_WHEN_logged_THEN_admin_gets_the_setup_guide() {
        guideLogger.log(DiscordConnection.Disabled)

        assertEquals(listOf(LogLine(LogLine.Level.INFO, translation.discord.guide.toMessengerText())), logger.lines)
    }

    @Test
    fun GIVEN_bot_with_a_token_WHEN_logged_THEN_nothing_is_written() {
        guideLogger.log(DiscordConnection.Connecting)
        guideLogger.log(DiscordConnection.Failed(DiscordFailure.InvalidToken))

        assertTrue(logger.lines.isEmpty())
    }
}
