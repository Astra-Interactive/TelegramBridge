package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.api.brigadier.command.PaperMultiplatformCommands
import ru.astrainteractive.astralibs.command.api.registrar.PaperCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.DefaultBukkitDispatchers
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.BukkitPlatformServer
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.MessageBridge
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.api.di.CoreModule
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannelImpl
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.di.BukkitMessengerModule
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule
import kotlin.time.Duration.Companion.seconds

class RootModule(
    plugin: MessageBridge
) : Logger by JUtiltLogger("MessageBridge-RootModule") {

    val bukkitCoreModule = BukkitCoreModule(plugin)

    val coreModule = CoreModule(
        dataFolder = bukkitCoreModule.plugin.dataFolder,
        dispatchers = DefaultBukkitDispatchers(bukkitCoreModule.plugin),
        platformServer = BukkitPlatformServer(),
        multiplatformCommand = MultiplatformCommand(PaperMultiplatformCommands()),
        commandRegistrarContextFactory = { mainScope ->
            PaperCommandRegistrarContext(
                mainScope = mainScope,
                plugin = plugin
            )
        }
    )

    private val bEventChannel: BEventChannel = BEventChannelImpl()

    val linkModule = LinkModule(coreModule, LuckPermsProvider.Default)

    val bukkitMessengerModule = BukkitMessengerModule(
        coreModule = coreModule,
        bukkitCoreModule = bukkitCoreModule,
        bEventChannel = bEventChannel,
        textInterceptors = listOf(linkModule.linkedNameInterceptor)
    )

    val jdaMessengerModule = JdaMessengerModule(
        coreModule = coreModule,
        bEventChannel = bEventChannel,
        messageInterceptors = listOf(linkModule.discordLinkInterceptor),
        authorResolver = linkModule.discordAuthorResolver,
        memberLeaveListeners = listOf(linkModule.discordMemberLeaveListener),
        roleChanges = linkModule.discordRoleChanges
    )

    val telegramMessengerModule = TelegramMessengerModule(
        coreModule = coreModule,
        bEventChannel = bEventChannel,
        messageInterceptors = listOf(linkModule.telegramLinkInterceptor)
    )

    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            lifecyclePlugin = plugin,
            commandRegistrarContext = coreModule.commandRegistrarContext
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            bukkitMessengerModule.lifecycle,
            jdaMessengerModule.lifecycle,
            telegramMessengerModule.lifecycle,
            linkModule.lifecycle,
            commandModule.lifecycle
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            coreModule.ioScope.launch {
                bEventChannel.consume(ServerOpenBEvent)
            }
            lifecycles.forEach(Lifecycle::onEnable)
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            runBlocking {
                withTimeoutOrNull(SERVER_CLOSED_TIMEOUT) {
                    listOf(telegramMessengerModule.bEventConsumer, jdaMessengerModule.bEventConsumer)
                        .map { consumer -> coreModule.ioScope.launch { consumer.consume(ServerClosedBEvent) } }
                        .joinAll()
                } ?: warn { "#onDisable messengers did not consume $ServerClosedBEvent within $SERVER_CLOSED_TIMEOUT" }
            }
            lifecycles.reversed().forEach(Lifecycle::onDisable)
        }
    )

    private companion object {
        val SERVER_CLOSED_TIMEOUT = 5.seconds
    }
}
