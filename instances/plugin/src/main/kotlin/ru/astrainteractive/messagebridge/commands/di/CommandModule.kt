package ru.astrainteractive.messagebridge.commands.di

import CommandManager
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule

class CommandModule(
    coreModule: CoreModule,
) {
    private val commandManager by lazy {
        CommandManager(
            translationKrate = coreModule.translationKrate,
            kyoriKrate = coreModule.kyoriKrate,
            plugin = coreModule.plugin
        )
    }

    val lifecycle = Lifecycle.Lambda(
        onEnable = { commandManager }
    )
}
