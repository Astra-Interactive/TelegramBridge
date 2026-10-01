@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.messenger.telegram.impl.connection.internal

import ru.astrainteractive.messagebridge.core.api.config.PluginTranslation
import ru.astrainteractive.messagebridge.core.api.fake.FakeTranslationKrate
import ru.astrainteractive.messagebridge.core.api.fake.LogLine
import ru.astrainteractive.messagebridge.core.api.fake.RecordingLogger
import ru.astrainteractive.messagebridge.core.api.mapping.toMessengerText
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramConnectionState
import ru.astrainteractive.messagebridge.messenger.telegram.api.TelegramFailure
import ru.astrainteractive.messagebridge.messenger.telegram.impl.failure.mapping.TelegramFailureTextMapperImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramStatusLoggerTest {
    private val translation = PluginTranslation()
    private val translationKrate = FakeTranslationKrate(translation)
    private val logger = RecordingLogger()
    private val statusLogger = TelegramStatusLogger(
        translationKrate = translationKrate,
        failureTextMapper = TelegramFailureTextMapperImpl(translationKrate = translationKrate),
        logger = logger
    )

    @Test
    fun GIVEN_no_token_WHEN_logged_THEN_it_is_only_a_verbose_line() {
        statusLogger.log(TelegramConnectionState.Disabled)

        assertEquals(listOf(LogLine.Level.VERBOSE), logger.lines.map { line -> line.level })
    }

    @Test
    fun GIVEN_connected_bot_WHEN_logged_THEN_admin_is_told_its_name() {
        statusLogger.log(TelegramConnectionState.Connected("@MyBridgeBot"))

        val connected = translation.telegram.status.connected("@MyBridgeBot").toMessengerText()
        assertEquals(listOf(LogLine(LogLine.Level.INFO, connected)), logger.lines)
    }

    @Test
    fun GIVEN_failed_bot_WHEN_logged_THEN_admin_gets_an_error_with_what_to_do() {
        statusLogger.log(TelegramConnectionState.Failed(TelegramFailure.InvalidToken))

        val invalidToken = translation.telegram.errors.invalidToken.toMessengerText()
        assertEquals(listOf(LogLine(LogLine.Level.ERROR, invalidToken)), logger.lines)
    }

    @Test
    fun GIVEN_connecting_bot_WHEN_logged_THEN_admin_is_not_bothered() {
        statusLogger.log(TelegramConnectionState.Connecting)

        assertTrue(logger.lines.all { line -> line.level == LogLine.Level.VERBOSE }, "${logger.lines}")
    }
}
