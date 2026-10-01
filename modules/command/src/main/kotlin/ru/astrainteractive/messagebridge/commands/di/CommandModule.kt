package ru.astrainteractive.messagebridge.commands.di

import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.commands.link.LinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.link.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.unlink.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.unlink.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.LinkModule

class CommandModule(
    private val coreModule: CoreModule,
    linkModule: LinkModule,
    private val commandRegistrarContext: CommandRegistrarContext
) {
    private val nodes = listOf(
        LinkLiteralArgumentBuilder(
            executor = LinkCommandExecutor(
                codeApi = linkModule.codeApi,
                linkingDao = linkModule.linkingDao,
                translationKrate = coreModule.translationKrate
            ),
            ioScope = coreModule.ioScope,
            multiplatformCommand = coreModule.multiplatformCommand,
            commandExceptionHandler = coreModule.commandExceptionHandler,
            platformServer = coreModule.platformServer
        ).create(),
        UnlinkLiteralArgumentBuilder(
            executor = UnlinkCommandExecutor(
                linkingDao = linkModule.linkingDao,
                luckPermsRoleController = linkModule.luckPermsRoleController,
                translationKrate = coreModule.translationKrate
            ),
            ioScope = coreModule.ioScope,
            multiplatformCommand = coreModule.multiplatformCommand,
            commandExceptionHandler = coreModule.commandExceptionHandler,
            platformServer = coreModule.platformServer
        ).create()
    )
    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            commandRegistrarContext.registerWhenReady(nodes, coreModule.unconfinedScope)
        }
    )
}
