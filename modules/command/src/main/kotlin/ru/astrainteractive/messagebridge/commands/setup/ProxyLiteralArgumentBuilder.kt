package ru.astrainteractive.messagebridge.commands.setup

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler

/** `proxy off` and `proxy <type> <host> <port> [username] [password] [--unsafe]` of a messenger. */
internal class ProxyLiteralArgumentBuilder(
    private val types: List<String>,
    private val readType: (String) -> PluginConfiguration.Proxy.Type,
    private val onProxy: (CommandContext<Any>, PluginConfiguration.Proxy?) -> Unit,
    private val values: SetupValues,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler
) {
    private fun requireProxy(
        ctx: CommandContext<Any>,
        typeArg: MultiplatformCommand.BrigadierArgument<String>,
        hostArg: MultiplatformCommand.BrigadierArgument<String>,
        portArg: MultiplatformCommand.BrigadierArgument<String>,
        credentials: String
    ): PluginConfiguration.Proxy = with(multiplatformCommand) {
        ctx.requirePermission(PluginPermission.Setup)
        values.proxy(
            sender = ctx.getSender(),
            type = readType.invoke(ctx.requireArgument(typeArg)),
            host = ctx.requireArgument(hostArg),
            port = ctx.requireArgument(portArg),
            credentials = SecretInput.parse(credentials)
        )
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("proxy") {
                literal("off") {
                    runs(commandExceptionHandler::handle) { ctx ->
                        ctx.requirePermission(PluginPermission.Setup)
                        onProxy.invoke(ctx, null)
                    }
                }
                argument("type", StringArgumentType.word()) { typeArg ->
                    hints { types }
                    argument("host", StringArgumentType.string()) { hostArg ->
                        argument("port", StringArgumentType.word()) { portArg ->
                            runs(commandExceptionHandler::handle) { ctx ->
                                onProxy.invoke(ctx, requireProxy(ctx, typeArg, hostArg, portArg, credentials = ""))
                            }
                            argument("credentials", StringArgumentType.greedyString()) { credentialsArg ->
                                runs(commandExceptionHandler::handle) { ctx ->
                                    val credentials = ctx.requireArgument(credentialsArg)
                                    onProxy.invoke(ctx, requireProxy(ctx, typeArg, hostArg, portArg, credentials))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
