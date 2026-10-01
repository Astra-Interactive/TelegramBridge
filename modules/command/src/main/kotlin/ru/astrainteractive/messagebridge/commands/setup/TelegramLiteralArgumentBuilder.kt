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
import ru.astrainteractive.messagebridge.messaging.setup.MessengerSetup
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus

internal class TelegramLiteralArgumentBuilder(
    setup: MessengerSetup,
    private val executor: SetupCommandExecutor,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    private val ioScope: CoroutineScope,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate
    private val values = SetupValues(translationKrate)
    private val telegram = SetupMessenger.telegram(setup)

    private fun launch(ctx: CommandContext<Any>, block: suspend (KCommandSender) -> Unit) {
        val sender = with(multiplatformCommand) { ctx.getSender() }
        ioScope.launch(commandExceptionHandler.coroutineExceptionHandler(ctx)) { block.invoke(sender) }
    }

    private fun saveProxy(ctx: CommandContext<Any>, proxy: PluginConfiguration.Proxy?) {
        val saved = proxy
            ?.let { translation.setup.saved.proxy(proxy.describe()) }
            ?: translation.setup.saved.proxyRemoved
        launch(ctx) { sender ->
            executor.saveAndConnect(sender, telegram, saved) { tgConfig -> tgConfig.copy(proxy = proxy) }
        }
    }

    private fun tokenCommand() = with(multiplatformCommand) {
        command("token") {
            argument("token", StringArgumentType.greedyString()) { tokenArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val input = SecretInput.parse(ctx.requireArgument(tokenArg))
                    values.requireSecretAllowed(ctx.getSender(), input)
                    val token = values.telegramToken(input)
                    val saved = translation.setup.saved.token(maskToken(token))
                    launch(ctx) { sender ->
                        executor.saveAndConnect(sender, telegram, saved) { tgConfig -> tgConfig.copy(token = token) }
                    }
                }
            }
        }
    }

    private fun chatCommand() = with(multiplatformCommand) {
        command("chat") {
            argument("chat_id", StringArgumentType.word()) { chatArg ->
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val chatId = values.chatId(ctx.requireArgument(chatArg))
                    val saved = translation.setup.saved.chat(chatId)
                    launch(ctx) { sender ->
                        executor.save(sender, telegram, saved) { tgConfig -> tgConfig.copy(chatID = chatId) }
                    }
                }
            }
        }
    }

    private fun topicCommand() = with(multiplatformCommand) {
        command("topic") {
            argument("topic_id", StringArgumentType.word()) { topicArg ->
                hints { listOf(SetupValues.NONE) }
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val topicId = values.topicId(ctx.requireArgument(topicArg))
                    val saved = if (topicId.isEmpty()) {
                        translation.setup.saved.topicRemoved
                    } else {
                        translation.setup.saved.topic(topicId)
                    }
                    launch(ctx) { sender ->
                        executor.save(sender, telegram, saved) { tgConfig -> tgConfig.copy(topicID = topicId) }
                    }
                }
            }
        }
    }

    private fun proxyCommand() = ProxyLiteralArgumentBuilder(
        types = listOf(SetupValues.HTTP, SetupValues.SOCKS5),
        readType = values::proxyType,
        onProxy = ::saveProxy,
        values = values,
        multiplatformCommand = multiplatformCommand,
        commandExceptionHandler = commandExceptionHandler
    ).create()

    private fun apiUrlCommand() = with(multiplatformCommand) {
        command("api-url") {
            argument("url", StringArgumentType.greedyString()) { urlArg ->
                hints { listOf(SetupValues.DEFAULT) }
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val url = values.apiUrl(ctx.requireArgument(urlArg).trim())
                    val saved = if (url.isEmpty()) {
                        translation.setup.saved.apiUrlRemoved
                    } else {
                        translation.setup.saved.apiUrl(withoutCredentials(url))
                    }
                    launch(ctx) { sender ->
                        executor.saveAndConnect(sender, telegram, saved) { tgConfig -> tgConfig.copy(apiUrl = url) }
                    }
                }
            }
        }
    }

    /** A group bot in privacy mode is sure to get only the commands that name it, like `/bind@MyBot`. */
    private fun bindMessageOf(code: String): String {
        val botName = (telegram.setup.status.value as? MessengerStatus.Connected)?.botName?.removePrefix("@")
        return if (botName.isNullOrBlank()) "/bind $code" else "/bind@$botName $code"
    }

    private fun bindCommand() = with(multiplatformCommand) {
        command("bind") {
            runs(commandExceptionHandler::handle) { ctx ->
                ctx.requirePermission(PluginPermission.Setup)
                executor.issueBindCode(ctx.getSender(), telegram) { code ->
                    translation.setup.telegramBindIssued(bindMessageOf(code), BindCodes.LIFETIME.inWholeMinutes)
                }
            }
        }
    }

    private fun checkCommand() = with(multiplatformCommand) {
        command("check") {
            runs(commandExceptionHandler::handle) { ctx ->
                ctx.requirePermission(PluginPermission.Setup)
                launch(ctx) { sender -> executor.check(sender, telegram) }
            }
        }
    }

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("telegram") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    if (telegram.setup.status.value !is MessengerStatus.Connected) {
                        sender.sendMessage(translation.telegram.guide)
                    }
                    executor.telegramStatus(telegram).forEach(sender::sendMessage)
                }
                then(tokenCommand())
                then(chatCommand())
                then(topicCommand())
                then(proxyCommand())
                then(apiUrlCommand())
                then(bindCommand())
                then(checkCommand())
            }
        }
    }
}
