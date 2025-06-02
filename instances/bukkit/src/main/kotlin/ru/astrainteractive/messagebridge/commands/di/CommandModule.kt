package ru.astrainteractive.messagebridge.commands.di

import CommandManager
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.messagebridge.commands.LinkCommandRegistry
import ru.astrainteractive.messagebridge.link.di.LinkModule

class CommandModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    linkModule: LinkModule,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>
) {
    private val commandManager by lazy {
        CommandManager(
            translationKrate = coreModule.translationKrate,
            kyoriKrate = kyoriKrate,
            plugin = bukkitCoreModule.plugin
        )
    }
    private val linkCommandRegistry = LinkCommandRegistry(
        translationKrate = coreModule.translationKrate,
        kyoriKrate = kyoriKrate,
        plugin = bukkitCoreModule.plugin,
        scope = coreModule.scope,
        codeApi = linkModule.codeApi,
        linkingDao = linkModule.linkingDao
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            commandManager
            linkCommandRegistry
        }
    )
}
