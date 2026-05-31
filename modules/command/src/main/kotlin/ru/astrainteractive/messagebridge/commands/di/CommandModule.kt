package ru.astrainteractive.messagebridge.commands.di

import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.commands.link.LinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.link.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.reload.ReloadLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.unlink.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.unlink.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule

class CommandModule(
    lifecyclePlugin: Lifecycle,
    coreModule: CoreModule,
    linkModule: LinkModule,
    private val commandRegistrarContext: CommandRegistrarContext,
    private val multiplatformCommand: MultiplatformCommand
) {
    private val nodes = listOf(
        ReloadLiteralArgumentBuilder(
            plugin = lifecyclePlugin,
            translationKrate = coreModule.translationKrate,
            kyoriKrate = coreModule.kyoriKrate,
            multiplatformCommand = multiplatformCommand
        ).create(),
        LinkLiteralArgumentBuilder(
            executor = LinkCommandExecutor(
                ioScope = coreModule.ioScope,
                codeApi = linkModule.codeApi,
                linkingDao = linkModule.linkingDao,
                translationKrate = coreModule.translationKrate,
                kyoriKrate = coreModule.kyoriKrate
            ),
            multiplatformCommand = multiplatformCommand,
            platformServer = coreModule.platformServer
        ).create(),
        UnlinkLiteralArgumentBuilder(
            executor = UnlinkCommandExecutor(
                ioScope = coreModule.ioScope,
                linkingDao = linkModule.linkingDao,
                translationKrate = coreModule.translationKrate,
                kyoriKrate = coreModule.kyoriKrate
            ),
            multiplatformCommand = multiplatformCommand,
            platformServer = coreModule.platformServer
        ).create()
    )
    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            nodes.forEach(commandRegistrarContext::registerWhenReady)
        }
    )
}
