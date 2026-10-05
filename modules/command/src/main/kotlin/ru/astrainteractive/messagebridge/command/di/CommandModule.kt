package ru.astrainteractive.messagebridge.command.di

import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.command.reload.ReloadLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.core.api.di.CoreModule

class CommandModule(
    lifecyclePlugin: Lifecycle,
    private val coreModule: CoreModule,
    private val commandRegistrarContext: CommandRegistrarContext
) {
    private val nodes = listOf(
        ReloadLiteralArgumentBuilder(
            plugin = lifecyclePlugin,
            translationKrate = coreModule.translationKrate,
            multiplatformCommand = coreModule.multiplatformCommand,
            commandExceptionHandler = coreModule.commandExceptionHandler
        ).create()
    )
    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            commandRegistrarContext.registerWhenReady(nodes, coreModule.unconfinedScope)
        }
    )
}
