package ru.astrainteractive.messagebridge.onboarding.impl.telegram.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.setting.command.SettingCommand
import ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal.TelegramMessenger
import ru.astrainteractive.messagebridge.onboarding.impl.telegram.internal.TelegramSettings

internal class TelegramSettingLiteralArgumentBuilder(
    private val messenger: TelegramMessenger,
    private val settings: TelegramSettings,
    private val settingCommand: SettingCommand,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    private fun tokenCommand() = with(multiplatformCommand) {
        command("token") {
            argument("token", StringArgumentType.greedyString()) { tokenArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    val setting = settings.token(ctx.getSender(), ctx.requireArgument(tokenArg))
                    settingCommand.saveAndConnect(ctx, messenger, setting)
                }
            }
        }
    }

    private fun chatCommand() = with(multiplatformCommand) {
        command("chat") {
            argument("chat_id", StringArgumentType.word()) { chatArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settingCommand.save(ctx, messenger, settings.chat(ctx.requireArgument(chatArg)))
                }
            }
        }
    }

    private fun topicCommand() = with(multiplatformCommand) {
        command("topic") {
            argument("topic_id", StringArgumentType.word()) { topicArg ->
                hints { _ -> listOf(TelegramSettings.NONE) }
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settingCommand.save(ctx, messenger, settings.topic(ctx.requireArgument(topicArg)))
                }
            }
        }
    }

    private fun apiUrlCommand() = with(multiplatformCommand) {
        command("api-url") {
            argument("url", StringArgumentType.greedyString()) { urlArg ->
                hints { _ -> listOf(TelegramSettings.DEFAULT) }
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(OnboardingPermission.Setup)
                    settingCommand.saveAndConnect(ctx, messenger, settings.apiUrl(ctx.requireArgument(urlArg)))
                }
            }
        }
    }

    fun create(): List<LiteralArgumentBuilder<Any>> {
        return listOf(tokenCommand(), chatCommand(), topicCommand(), apiUrlCommand())
    }
}
