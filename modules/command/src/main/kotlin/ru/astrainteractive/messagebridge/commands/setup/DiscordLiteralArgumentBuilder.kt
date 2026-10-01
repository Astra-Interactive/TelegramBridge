package ru.astrainteractive.messagebridge.commands.setup

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.sender.KCommandSender
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginConfiguration
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.messaging.setup.BindCodes
import ru.astrainteractive.messagebridge.messaging.setup.DiscordSetup
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus

internal class DiscordLiteralArgumentBuilder(
    private val setup: DiscordSetup,
    private val executor: SetupCommandExecutor,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    private val ioScope: CoroutineScope,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate
    private val values = SetupValues(translationKrate)
    private val discord = SetupMessenger.discord(setup)

    private fun launch(ctx: CommandContext<Any>, block: suspend (KCommandSender) -> Unit) {
        val sender = with(multiplatformCommand) { ctx.getSender() }
        ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) { block.invoke(sender) }
    }

    private fun saveProxy(ctx: CommandContext<Any>, proxy: PluginConfiguration.Proxy?) {
        val saved = proxy
            ?.let { translation.setup.saved.proxy(proxy.describe()) }
            ?: translation.setup.saved.proxyRemoved
        launch(ctx) { sender ->
            executor.saveAndConnect(sender, discord, saved) { jdaConfig -> jdaConfig.copy(proxy = proxy) }
        }
    }

    private fun tokenCommand() = with(multiplatformCommand) {
        command("token") {
            argument("token", StringArgumentType.greedyString()) { tokenArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val input = SecretInput.parse(ctx.requireArgument(tokenArg))
                    values.requireSecretAllowed(ctx.getSender(), input)
                    val token = values.discordToken(input)
                    val saved = translation.setup.saved.token(maskToken(token))
                    launch(ctx) { sender ->
                        executor.saveAndConnect(sender, discord, saved) { jdaConfig -> jdaConfig.copy(token = token) }
                    }
                }
            }
        }
    }

    private fun channelCommand() = with(multiplatformCommand) {
        command("channel") {
            argument("channel_id", StringArgumentType.word()) { channelArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val channelId = values.channelId(ctx.requireArgument(channelArg))
                    val saved = translation.setup.saved.channel(channelId)
                    launch(ctx) { sender ->
                        executor.save(sender, discord, saved) { jdaConfig -> jdaConfig.copy(channelId = channelId) }
                    }
                }
            }
        }
    }

    private fun activityCommand() = with(multiplatformCommand) {
        command("activity") {
            argument("text", StringArgumentType.greedyString()) { textArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val activity = ctx.requireArgument(textArg).trim()
                    val saved = translation.setup.saved.activity(activity)
                    launch(ctx) { sender ->
                        executor.saveAndConnect(sender, discord, saved) { jdaConfig ->
                            jdaConfig.copy(activity = activity)
                        }
                    }
                }
            }
        }
    }

    private fun proxyCommand() = ProxyLiteralArgumentBuilder(
        types = listOf(SetupValues.HTTP),
        readType = values::discordProxyType,
        onProxy = ::saveProxy,
        values = values,
        multiplatformCommand = multiplatformCommand,
        commandExceptionHandler = commandExceptionHandler
    ).create()

    private fun bindCommand() = with(multiplatformCommand) {
        command("bind") {
            runs(commandExceptionHandler::handle) { ctx ->
                ctx.requirePermission(PluginPermission.Setup)
                executor.issueBindCode(ctx.getSender(), discord) { code ->
                    translation.setup.discordBindIssued(code, BindCodes.LIFETIME.inWholeMinutes)
                }
            }
        }
    }

    private fun inviteCommand() = with(multiplatformCommand) {
        command("invite") {
            runs(commandExceptionHandler::handle) { ctx ->
                ctx.requirePermission(PluginPermission.Setup)
                launch(ctx) { sender -> executor.invite(sender, discord, setup) }
            }
        }
    }

    private fun checkCommand() = with(multiplatformCommand) {
        command("check") {
            runs(commandExceptionHandler::handle) { ctx ->
                ctx.requirePermission(PluginPermission.Setup)
                launch(ctx) { sender -> executor.check(sender, discord) }
            }
        }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("discord") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    if (setup.status.value !is MessengerStatus.Connected) {
                        sender.sendMessage(translation.discord.guide)
                    }
                    executor.discordStatus(discord).forEach(sender::sendMessage)
                }
                then(tokenCommand())
                then(channelCommand())
                then(activityCommand())
                then(proxyCommand())
                then(bindCommand())
                then(inviteCommand())
                then(checkCommand())
            }
        }
    }
}
