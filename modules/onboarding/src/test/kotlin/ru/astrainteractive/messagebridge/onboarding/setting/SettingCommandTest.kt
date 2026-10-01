@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.onboarding.setting

import com.mojang.brigadier.CommandDispatcher
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.onboarding.OnboardingFixture
import ru.astrainteractive.messagebridge.onboarding.status.StatusText
import ru.astrainteractive.messagebridge.onboarding.telegram.TelegramMessenger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SettingCommandTest {
    private val fixture = OnboardingFixture()
    private val core = fixture.coreModule
    private val messenger = TelegramMessenger(fixture.telegram)
    private val settingCommand = SettingCommand(
        saver = SettingSaver(
            configKrate = core.configKrate,
            config = core.config,
            statusText = StatusText(core.translationKrate),
            connectionTimeout = 20.seconds,
            translationKrate = core.translationKrate
        ),
        ioScope = core.ioScope,
        multiplatformCommand = core.multiplatformCommand,
        commandExceptionHandler = core.commandExceptionHandler
    )

    @AfterTest
    fun cleanup() {
        fixture.close()
    }

    private fun execute(setting: Result<Setting<PluginConfiguration.TelegramConfig>>) {
        val dispatcher = CommandDispatcher<Any>()
        val node = with(core.multiplatformCommand) {
            command("set") {
                runs { ctx -> settingCommand.save(ctx, messenger, setting) }
            }
        }
        dispatcher.register(node)
        dispatcher.execute("set", fixture.console)
    }

    @Test
    fun GIVEN_failure_that_is_not_a_refused_value_WHEN_setting_is_applied_THEN_reads_the_unknown_error() {
        execute(Result.failure(IllegalStateException("a bug in a parser")))

        assertEquals(listOf(fixture.plainTextOf(fixture.translation.commandError.unknownError)), fixture.consoleReplies)
    }
}
