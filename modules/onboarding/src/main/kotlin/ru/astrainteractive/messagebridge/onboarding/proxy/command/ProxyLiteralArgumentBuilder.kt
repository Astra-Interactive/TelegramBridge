package ru.astrainteractive.messagebridge.onboarding.proxy.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.permission.PluginPermission
import ru.astrainteractive.messagebridge.onboarding.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.proxy.api.ProxyTypes
import ru.astrainteractive.messagebridge.onboarding.proxy.internal.ProxySettings
import ru.astrainteractive.messagebridge.onboarding.setting.command.SettingCommand
import ru.astrainteractive.messagebridge.onboarding.setting.model.Setting

internal class ProxyLiteralArgumentBuilder<C>(
    private val messenger: Messenger<C>,
    private val types: ProxyTypes,
    private val settings: ProxySettings,
    private val settingCommand: SettingCommand,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    private fun settingOf(
        ctx: CommandContext<Any>,
        typeArg: MultiplatformCommand.BrigadierArgument<String>,
        hostArg: MultiplatformCommand.BrigadierArgument<String>,
        portArg: MultiplatformCommand.BrigadierArgument<String>,
        credentials: String
    ): Result<Setting<C>> {
        val type = with(multiplatformCommand) { types.read(ctx.requireArgument(typeArg)) }
            .getOrElse { error -> return Result.failure(error) }
        val proxy = with(multiplatformCommand) {
            settings.parse(
                sender = ctx.getSender(),
                type = type,
                host = ctx.requireArgument(hostArg),
                port = ctx.requireArgument(portArg),
                credentials = credentials
            )
        }
        return proxy.map { validProxy -> settings.settingOf(messenger, validProxy) }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("proxy") {
                literal(OFF) {
                    runs(commandExceptionHandler::handle) { ctx ->
                        ctx.requirePermission(PluginPermission.Setup)
                        val setting = Result.success(settings.settingOf(messenger, proxy = null))
                        settingCommand.saveAndConnect(ctx, messenger, setting)
                    }
                }
                argument("type", StringArgumentType.word()) { typeArg ->
                    hints { _ -> types.keywords }
                    argument("host", StringArgumentType.string()) { hostArg ->
                        argument("port", StringArgumentType.word()) { portArg ->
                            runs(commandExceptionHandler::handle) { ctx ->
                                ctx.requirePermission(PluginPermission.Setup)
                                val setting = settingOf(ctx, typeArg, hostArg, portArg, credentials = "")
                                settingCommand.saveAndConnect(ctx, messenger, setting)
                            }
                            argument("credentials", StringArgumentType.greedyString()) { credentialsArg ->
                                runs(commandExceptionHandler::handle) { ctx ->
                                    ctx.requirePermission(PluginPermission.Setup)
                                    val credentials = ctx.requireArgument(credentialsArg)
                                    val setting = settingOf(ctx, typeArg, hostArg, portArg, credentials)
                                    settingCommand.saveAndConnect(ctx, messenger, setting)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val OFF = "off"
    }
}
