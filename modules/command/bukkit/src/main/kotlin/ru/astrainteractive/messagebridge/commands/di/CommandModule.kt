package ru.astrainteractive.messagebridge.commands.di

import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.messagebridge.commands.link.LinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.link.LinkCommandRegistry
import ru.astrainteractive.messagebridge.commands.reload.ReloadCommandRegistry
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule

class CommandModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    linkModule: LinkModule,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>
) {
    private val commandRegistrarContext = PaperCommandRegistrarContext(
        coreModule.mainScope,
        bukkitCoreModule.plugin
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            ReloadCommandRegistry(
                plugin = bukkitCoreModule.plugin,
                translationKrate = coreModule.translationKrate,
                kyoriKrate = kyoriKrate,
                commandRegistrarContext = commandRegistrarContext
            ).register()
            LinkCommandRegistry(
                commandRegistrarContext = commandRegistrarContext,
                executor = LinkCommandExecutor(
                    scope = coreModule.ioScope,
                    codeApi = linkModule.codeApi,
                    linkingDao = linkModule.linkingDao,
                    translationKrate = coreModule.translationKrate,
                    kyoriKrate = kyoriKrate
                )
            ).register()
        }
    )
}
