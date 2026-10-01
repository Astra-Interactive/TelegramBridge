package ru.astrainteractive.messagebridge.commands.di

import ru.astrainteractive.astralibs.command.api.registrar.CommandRegistrarContext
import ru.astrainteractive.astralibs.command.api.registrar.registerWhenReady
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.commands.link.LinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.link.LinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.reload.ReloadLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.setup.DiscordLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.setup.MbLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.setup.SetupCommandExecutor
import ru.astrainteractive.messagebridge.commands.setup.StatusLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.setup.TelegramLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.commands.unlink.UnlinkCommandExecutor
import ru.astrainteractive.messagebridge.commands.unlink.UnlinkLiteralArgumentBuilder
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messaging.setup.DiscordSetup
import ru.astrainteractive.messagebridge.messaging.setup.MessengerSetup

class CommandModule(
    lifecyclePlugin: Lifecycle,
    private val coreModule: CoreModule,
    linkModule: LinkModule,
    telegramSetup: MessengerSetup,
    discordSetup: DiscordSetup,
    private val commandRegistrarContext: CommandRegistrarContext
) {
    private val setupCommandExecutor = SetupCommandExecutor(
        configFile = coreModule.configFile,
        configKrate = coreModule.configKrate,
        translationKrate = coreModule.translationKrate
    )

    private val mbCommand = MbLiteralArgumentBuilder(
        subcommands = listOf(
            StatusLiteralArgumentBuilder(
                telegramSetup = telegramSetup,
                discordSetup = discordSetup,
                executor = setupCommandExecutor,
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                translationKrate = coreModule.translationKrate
            ).create(),
            ReloadLiteralArgumentBuilder(
                plugin = lifecyclePlugin,
                files = listOf(coreModule.configFile, coreModule.translationFile),
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                translationKrate = coreModule.translationKrate
            ).create(),
            TelegramLiteralArgumentBuilder(
                setup = telegramSetup,
                executor = setupCommandExecutor,
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                ioScope = coreModule.ioScope,
                translationKrate = coreModule.translationKrate
            ).create(),
            DiscordLiteralArgumentBuilder(
                setup = discordSetup,
                executor = setupCommandExecutor,
                multiplatformCommand = coreModule.multiplatformCommand,
                commandExceptionHandler = coreModule.commandExceptionHandler,
                ioScope = coreModule.ioScope,
                translationKrate = coreModule.translationKrate
            ).create()
        ),
        multiplatformCommand = coreModule.multiplatformCommand,
        commandExceptionHandler = coreModule.commandExceptionHandler,
        translationKrate = coreModule.translationKrate
    )

    private val nodes = listOf(
        mbCommand.create(),
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
