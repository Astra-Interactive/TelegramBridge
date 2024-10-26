package ru.astrainteractive.messagebridge.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.core.util.getValue
import ru.astrainteractive.messagebridge.link.api.CodeApi
import ru.astrainteractive.messagebridge.link.api.model.CodeUser

internal class LinkCommandRegistry(
    translationKrate: Krate<PluginTranslation>,
    kyoriKrate: Krate<KyoriComponentSerializer>,
    private val plugin: LifecyclePlugin,
    private val scope: CoroutineScope,
    private val codeApi: CodeApi
) {
    private val translation by translationKrate
    private val kyori by kyoriKrate

    private fun register() = plugin.getCommand("link")?.setExecutor { sender, command, label, args ->
        val player = sender as? Player ?: return@setExecutor true
        with(kyori) {
            scope.launch {
                val codeUser = CodeUser(
                    name = player.name,
                    uuid = player.uniqueId
                )
                val code = codeApi.generateCodeForPlayer(codeUser)
                player.sendMessage(translation.link.codeCreated(code).component)
            }
        }
        true
    }

    init {
        register()
    }
}
