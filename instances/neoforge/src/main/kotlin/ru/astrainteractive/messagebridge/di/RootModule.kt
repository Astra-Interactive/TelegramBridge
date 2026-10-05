package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import net.neoforged.fml.loading.FMLPaths
import ru.astrainteractive.astralibs.command.api.brigadier.command.MultiplatformCommand
import ru.astrainteractive.astralibs.command.brigadier.command.MinecraftMultiplatformCommands
import ru.astrainteractive.astralibs.command.registrar.NeoForgeCommandRegistrarContext
import ru.astrainteractive.astralibs.coroutines.MinecraftDispatchers
import ru.astrainteractive.astralibs.lifecycle.ForgeLifecycleServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.server.bridge.MinecraftPlatformServer
import ru.astrainteractive.astralibs.server.permission.LuckPermsProvider
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.di.LinkModule
import ru.astrainteractive.messagebridge.messenger.api.api.BEventChannel
import ru.astrainteractive.messagebridge.messenger.api.impl.BEventChannelImpl
import ru.astrainteractive.messagebridge.messenger.api.model.ServerClosedBEvent
import ru.astrainteractive.messagebridge.messenger.api.model.ServerOpenBEvent
import ru.astrainteractive.messagebridge.messenger.discord.di.JdaMessengerModule
import ru.astrainteractive.messagebridge.messenger.neoforge.di.NeoForgeMessengerModule
import ru.astrainteractive.messagebridge.messenger.telegram.di.TelegramMessengerModule
import java.io.File
import kotlin.time.Duration.Companion.seconds

class RootModule(
    forgeLifecycleServer: ForgeLifecycleServer
) : Logger by JUtiltLogger("MessageBridge-RootModule") {
    val coreModule = CoreModule(
        dataFolder = FMLPaths.CONFIGDIR.get()
            .resolve("MessageBridge")
            .toAbsolutePath()
            .toFile()
            .also(File::mkdirs),
        dispatchers = MinecraftDispatchers(),
        platformServer = MinecraftPlatformServer,
        multiplatformCommand = MultiplatformCommand(MinecraftMultiplatformCommands()),
        commandRegistrarContextFactory = ::NeoForgeCommandRegistrarContext
    )

    private val bEventChannel: BEventChannel = BEventChannelImpl()

    val linkModule by lazy {
        LinkModule(coreModule, LuckPermsProvider.Default)
    }

    val neoForgeMessengerModule by lazy {
        NeoForgeMessengerModule(
            coreModule = coreModule,
            bEventChannel = bEventChannel,
        )
    }
    val commandModule by lazy {
        CommandModule(
            coreModule = coreModule,
            lifecyclePlugin = forgeLifecycleServer,
            commandRegistrarContext = coreModule.commandRegistrarContext
        )
    }

    val jdaEventModule by lazy {
        JdaMessengerModule(
            coreModule = coreModule,
            bEventChannel = bEventChannel,
            messageInterceptors = listOf(linkModule.discordLinkInterceptor),
            authorResolver = linkModule.discordAuthorResolver,
            memberLeaveListeners = listOf(linkModule.discordMemberLeaveListener),
            roleChanges = linkModule.discordRoleChanges
        )
    }

    val tgEventModule by lazy {
        TelegramMessengerModule(
            coreModule = coreModule,
            bEventChannel = bEventChannel,
            messageInterceptors = listOf(linkModule.telegramLinkInterceptor)
        )
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            linkModule.lifecycle,
            commandModule.lifecycle,
            jdaEventModule.lifecycle,
            tgEventModule.lifecycle,
            neoForgeMessengerModule.lifecycle
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
                    listOf(tgEventModule.bEventConsumer, jdaEventModule.bEventConsumer)
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
