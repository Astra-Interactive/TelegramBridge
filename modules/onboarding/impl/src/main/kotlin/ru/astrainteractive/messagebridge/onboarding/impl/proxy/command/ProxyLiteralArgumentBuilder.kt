package ru.astrainteractive.messagebridge.onboarding.impl.proxy.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.server.KAudience
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.MutableKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.api.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.core.api.config.PluginConfiguration
import ru.astrainteractive.messagebridge.core.api.util.describe
import ru.astrainteractive.messagebridge.onboarding.api.config.OnboardingTranslation
import ru.astrainteractive.messagebridge.onboarding.api.permission.OnboardingPermission
import ru.astrainteractive.messagebridge.onboarding.impl.api.Messenger
import ru.astrainteractive.messagebridge.onboarding.impl.proxy.api.ProxyTypes
import ru.astrainteractive.messagebridge.onboarding.impl.proxy.internal.ProxySettings
import ru.astrainteractive.messagebridge.onboarding.impl.proxy.mapping.proxyOf

internal class ProxyLiteralArgumentBuilder(
    private val messenger: Messenger,
    private val types: ProxyTypes,
    private val settings: ProxySettings,
    private val configKrate: MutableKrate<Result<PluginConfiguration>>,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<OnboardingTranslation>
) {
    private val translation by translationKrate

    private fun readProxy(
        ctx: CommandContext<Any>,
        typeArg: MultiplatformCommand.BrigadierArgument<String>,
        hostArg: MultiplatformCommand.BrigadierArgument<String>,
        portArg: MultiplatformCommand.BrigadierArgument<String>,
        credentials: String
    ): Result<PluginConfiguration.Proxy> {
        val type = with(multiplatformCommand) { types.read(ctx.requireArgument(typeArg)) }
            .getOrElse { t -> return Result.failure(t) }
        return with(multiplatformCommand) {
            settings.parse(
                sender = ctx.getSender(),
                type = type,
                host = ctx.requireArgument(hostArg),
                port = ctx.requireArgument(portArg),
                credentials = credentials
            )
        }
    }

    private fun save(sender: KAudience, proxy: PluginConfiguration.Proxy?) {
        configKrate
            .saveAndGet { loaded -> loaded.map { config -> messenger.withProxy(config, proxy) } }
            .onSuccess { _ -> sender.sendMessage(translation.setup.saved.proxyOf(proxy)) }
            .onFailure { t -> sender.sendMessage(translation.setup.configBroken(t.describe())) }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("proxy") {
                literal(OFF) {
                    runs(commandExceptionHandler::handle) { ctx ->
                        ctx.requirePermission(OnboardingPermission.Setup)
                        save(ctx.getSender(), proxy = null)
                    }
                }
                argument("type", StringArgumentType.word()) { typeArg ->
                    hints { _ -> types.keywords }
                    argument("host", StringArgumentType.string()) { hostArg ->
                        argument("port", StringArgumentType.word()) { portArg ->
                            runs(commandExceptionHandler::handle) { ctx ->
                                ctx.requirePermission(OnboardingPermission.Setup)
                                readProxy(ctx, typeArg, hostArg, portArg, credentials = "")
                                    .onSuccess { proxy -> save(ctx.getSender(), proxy) }
                                    .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
                            }
                            argument("credentials", StringArgumentType.greedyString()) { credentialsArg ->
                                runs(commandExceptionHandler::handle) { ctx ->
                                    ctx.requirePermission(OnboardingPermission.Setup)
                                    val credentials = ctx.requireArgument(credentialsArg)
                                    readProxy(ctx, typeArg, hostArg, portArg, credentials)
                                        .onSuccess { proxy -> save(ctx.getSender(), proxy) }
                                        .onFailure { t -> commandExceptionHandler.handle(ctx, t) }
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
