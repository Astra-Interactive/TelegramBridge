package ru.astrainteractive.messagebridge.onboarding.discord

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.onboarding.setting.SettingCommand

/** `/mb discord token|channel|activity <value>`. */
internal class DiscordSettingLiteralArgumentBuilder(
    private val messenger: DiscordMessenger,
    private val settings: DiscordSettings,
    private val settingCommand: SettingCommand,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    private fun tokenCommand() = with(multiplatformCommand) {
        command("token") {
            argument("token", StringArgumentType.greedyString()) { tokenArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val setting = settings.token(ctx.getSender(), ctx.requireArgument(tokenArg))
                    settingCommand.saveAndConnect(ctx, messenger, setting)
                }
            }
        }
    }

    private fun channelCommand() = with(multiplatformCommand) {
        command("channel") {
            argument("channel_id", StringArgumentType.word()) { channelArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    settingCommand.save(ctx, messenger, settings.channel(ctx.requireArgument(channelArg)))
                }
            }
        }
    }

    private fun activityCommand() = with(multiplatformCommand) {
        command("activity") {
            argument("text", StringArgumentType.greedyString()) { textArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    settingCommand.saveAndConnect(ctx, messenger, settings.activity(ctx.requireArgument(textArg)))
                }
            }
        }
    }

    fun create(): List<LiteralArgumentBuilder<Any>> {
        return listOf(tokenCommand(), channelCommand(), activityCommand())
    }
}
