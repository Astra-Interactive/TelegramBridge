package ru.astrainteractive.telegrambridge.commands

import CommandManager
import ru.astrainteractive.telegrambridge.TelegramBridge
import ru.astrainteractive.astralibs.commands.registerCommand
import ru.astrainteractive.telegrambridge.modules.Modules
import ru.astrainteractive.telegrambridge.utils.Permission

/**
 * Reload command handler
 */

/**
 * This function called only when atempreload being called
 *
 * Here you should also check for permission
 */
fun CommandManager.reload() = TelegramBridge.instance.registerCommand("tbreload") {
    val translation = Modules.TranslationModule.value
    if (!Permission.Reload.hasPermission(sender)) {
        sender.sendMessage(translation.noPermission)
        return@registerCommand
    }
    sender.sendMessage(translation.reload)
    TelegramBridge.instance.reloadPlugin()
    sender.sendMessage(translation.reloadComplete)
}






