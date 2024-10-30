package ru.astrainteractive.messagebridge.commands.di

import CommandManager
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.commands.LinkCommandRegistry
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule

class CommandModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    linkModule: LinkModule
) {
    private val commandManager by lazy {
        CommandManager(
            translationKrate = coreModule.translationKrate,
            kyoriKrate = coreModule.kyoriKrate,
            plugin = bukkitCoreModule.plugin
        )
    }
    private val linkCommandRegistry = LinkCommandRegistry(
        translationKrate = coreModule.translationKrate,
        kyoriKrate = coreModule.kyoriKrate,
        plugin = bukkitCoreModule.plugin,
        scope = coreModule.scope,
        codeApi = linkModule.codeApi
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            commandManager
            linkCommandRegistry
        }
    )
}
