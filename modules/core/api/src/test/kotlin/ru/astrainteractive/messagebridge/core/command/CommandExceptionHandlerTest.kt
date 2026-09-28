@file:Suppress("FunctionNaming")

package ru.astrainteractive.messagebridge.core.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.argumenttype.IntArgumentConverter
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.exception.BadArgumentException
import ru.astrainteractive.astralibs.command.api.exception.CommandException
import ru.astrainteractive.astralibs.command.api.exception.LocalizableComponentCommandException
import ru.astrainteractive.astralibs.command.api.exception.NoPermissionException
import ru.astrainteractive.astralibs.command.api.exception.NoPlayerException
import ru.astrainteractive.astralibs.command.api.exception.NoPotionEffectTypeException
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.astralibs.localization.text.LocalizedText
import ru.astrainteractive.klibs.kstorage.api.asCachedMutableKrate
import ru.astrainteractive.klibs.kstorage.api.impl.DefaultMutableKrate
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandExceptionHandlerTest {
    private val sender = RecordingConsoleKCommandSender()
    private val multiplatformCommand = MultiplatformCommand(FakeMultiplatformCommands(sender))
    private val translationKrate = DefaultMutableKrate(
        factory = ::PluginTranslation,
        loader = { null }
    ).asCachedMutableKrate()
    private val handler = CommandExceptionHandler(
        multiplatformCommand = multiplatformCommand,
        translationKrate = translationKrate
    )
    private val commandError = PluginTranslation().commandError

    private fun assertSenderReadOnly(message: LocalizableComponent) {
        assertEquals(listOf(message), sender.messages)
    }

    private fun execute(command: LiteralArgumentBuilder<Any>, input: String): Int {
        val dispatcher = CommandDispatcher<Any>()
        dispatcher.register(command)
        return dispatcher.execute(input, Any())
    }

    private fun failingCommand(
        multiplatformCommand: MultiplatformCommand,
        handler: CommandExceptionHandler,
        failure: Throwable
    ): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("fail") {
                runs(handler::handle) { _ -> throw failure }
            }
        }
    }

    private fun executeFailing(failure: Throwable) {
        execute(failingCommand(multiplatformCommand, handler, failure), "fail")
    }

    @Test
    fun GIVEN_sender_without_permission_WHEN_command_requires_it_THEN_sender_reads_no_permission() {
        val command = with(multiplatformCommand) {
            command("reload") {
                runs(handler::handle) { ctx -> ctx.requirePermission(PluginPermission.Reload) }
            }
        }

        execute(command, "reload")

        assertSenderReadOnly(commandError.noPermission)
    }

    @Test
    fun GIVEN_console_WHEN_command_requires_player_THEN_console_reads_only_player_command() {
        val command = with(multiplatformCommand) {
            command("link") {
                runs(handler::handle) { ctx -> ctx.requirePlayer() }
            }
        }

        execute(command, "link")

        assertSenderReadOnly(commandError.onlyPlayerCommand)
    }

    @Test
    fun GIVEN_unknown_player_WHEN_command_looks_them_up_THEN_sender_reads_player_not_found() {
        executeFailing(NoPlayerException("Notch"))

        assertSenderReadOnly(commandError.playerNotFound)
    }

    @Test
    fun GIVEN_argument_that_is_not_a_number_WHEN_command_converts_it_THEN_sender_reads_invalid_argument() {
        val command = with(multiplatformCommand) {
            command("pay") {
                argument("amount", StringArgumentType.string()) { amountArg ->
                    runs(handler::handle) { ctx -> ctx.requireArgument(amountArg, IntArgumentConverter) }
                }
            }
        }

        execute(command, "pay ten")

        assertSenderReadOnly(commandError.invalidArgument)
    }

    @Test
    fun GIVEN_argument_of_incompatible_type_WHEN_command_fails_THEN_sender_reads_invalid_argument() {
        executeFailing(BadArgumentException(wrongArgument = "ten", type = IntArgumentConverter))

        assertSenderReadOnly(commandError.invalidArgument)
    }

    @Test
    fun GIVEN_unknown_potion_effect_WHEN_command_looks_it_up_THEN_sender_reads_invalid_argument() {
        executeFailing(NoPotionEffectTypeException("flying"))

        assertSenderReadOnly(commandError.invalidArgument)
    }

    @Test
    fun GIVEN_generic_command_exception_WHEN_command_fails_THEN_sender_reads_wrong_usage() {
        executeFailing(CommandException("The source is not a player"))

        assertSenderReadOnly(commandError.wrongUsage)
    }

    @Test
    fun GIVEN_failure_with_its_own_text_WHEN_command_fails_THEN_sender_reads_that_text() {
        val text = LocalizedText.shared("Code expired")

        executeFailing(LocalizableComponentCommandException(text))

        assertSenderReadOnly(text)
    }

    @Test
    fun GIVEN_unexpected_exception_WHEN_command_fails_THEN_sender_reads_unknown_error() {
        executeFailing(IllegalStateException("Database is closed"))

        assertSenderReadOnly(commandError.unknownError)
    }

    @Test
    fun GIVEN_argument_missing_from_the_node_WHEN_command_reads_it_THEN_sender_reads_unknown_error() {
        val missingArgument = MultiplatformCommand.BrigadierArgument(
            alias = "player",
            type = StringArgumentType.string(),
            clazz = String::class.java
        )
        val command = with(multiplatformCommand) {
            command("unlink") {
                runs(handler::handle) { ctx -> ctx.requireArgument(missingArgument) }
            }
        }

        execute(command, "unlink")

        assertSenderReadOnly(commandError.unknownError)
    }

    @Test
    fun GIVEN_translations_reloaded_WHEN_command_fails_THEN_sender_reads_reloaded_text() {
        val reloadedText = LocalizedText.shared("Reloaded unknown error")
        translationKrate.save(
            PluginTranslation(commandError = PluginTranslation.CommandError(unknownError = reloadedText))
        )

        executeFailing(IllegalStateException("Database is closed"))

        assertSenderReadOnly(reloadedText)
    }

    @Test
    fun GIVEN_sender_the_platform_can_not_wrap_WHEN_command_fails_THEN_command_still_completes() {
        val commandBlockCommand = MultiplatformCommand(FakeMultiplatformCommands(sender = null))
        val commandBlockHandler = CommandExceptionHandler(
            multiplatformCommand = commandBlockCommand,
            translationKrate = translationKrate
        )
        val command = failingCommand(
            multiplatformCommand = commandBlockCommand,
            handler = commandBlockHandler,
            failure = NoPermissionException(PluginPermission.Reload)
        )

        assertEquals(Command.SINGLE_SUCCESS, execute(command, "fail"))
    }
}
