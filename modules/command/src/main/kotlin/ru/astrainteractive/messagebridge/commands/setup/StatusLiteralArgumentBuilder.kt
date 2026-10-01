package ru.astrainteractive.messagebridge.commands.setup

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.command.CommandExceptionHandler
import ru.astrainteractive.messagebridge.messaging.setup.MessengerSetup

internal class StatusLiteralArgumentBuilder(
    telegramSetup: MessengerSetup,
    discordSetup: MessengerSetup,
    private val executor: SetupCommandExecutor,
    private val multiplatformCommand: MultiplatformCommand,
    private val commandExceptionHandler: CommandExceptionHandler,
    translationKrate: CachedKrate<PluginTranslation>
) {
    private val translation by translationKrate
    private val telegram = SetupMessenger.telegram(telegramSetup)
    private val discord = SetupMessenger.discord(discordSetup)

    fun create(): LiteralArgumentBuilder<Any> {
        return with(multiplatformCommand) {
            command("status") {
                runs(commandExceptionHandler::handle) { ctx ->
                    ctx.requirePermission(PluginPermission.Setup)
                    val sender = ctx.getSender()
                    sender.sendMessage(translation.setup.status.header)
                    executor.telegramStatus(telegram).forEach(sender::sendMessage)
                    executor.discordStatus(discord).forEach(sender::sendMessage)
                }
            }
        }
    }
}
