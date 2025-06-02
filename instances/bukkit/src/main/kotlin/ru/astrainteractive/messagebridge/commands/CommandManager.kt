import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.astralibs.permission.BukkitPermissibleExt.toPermissible
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.util.getValue
import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.core.PluginPermission
import ru.astrainteractive.messagebridge.core.PluginTranslation

class CommandManager(
    translationKrate: CachedKrate<PluginTranslation>,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>,
    private val plugin: LifecyclePlugin
) : Logger by JUtiltLogger("CommandManager") {
    val translation by translationKrate
    val kyori by kyoriKrate

    private fun reload() = plugin.getCommand("mbreload")?.setExecutor { sender, command, label, args ->
        info { "#reload command" }
        if (!sender.toPermissible().hasPermission(PluginPermission.Reload)) {
            kyori
                .toComponent(translation.noPermission)
                .let(KyoriComponentSerializer.Plain.serializer::serialize)
                .run(sender::sendMessage)
            info { "#reload no permission" }
            return@setExecutor true
        }
        kyori
            .toComponent(translation.reload)
            .let(KyoriComponentSerializer.Plain.serializer::serialize)
            .run(sender::sendMessage)
        plugin.onReload()
        kyori
            .toComponent(translation.reloadComplete)
            .let(KyoriComponentSerializer.Plain.serializer::serialize)
            .run(sender::sendMessage)
        true
    }

    init {
        info { "#init" }
        reload()
    }
}
