import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.permission.BukkitPermissibleExt.toPermissible
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.utils.getValue

class CommandManager(
    translationKrate: Krate<PluginTranslation>,
    kyoriKrate: Krate<KyoriComponentSerializer>,
    private val plugin: MessageBridge
) {
    val translation by translationKrate
    val kyori by kyoriKrate

    val reloadCommand = plugin.getCommand("mbreload")?.setExecutor { sender, command, label, args ->
        if (!sender.toPermissible().hasPermission(PluginPermission.Reload)) {
            sender.sendMessage(translation.noPermission.let(kyori::toComponent))
            return@setExecutor true
        }
        sender.sendMessage(translation.reload.let(kyori::toComponent))
        plugin.onReload()
        sender.sendMessage(translation.reloadComplete.let(kyori::toComponent))
        true
    }
}
