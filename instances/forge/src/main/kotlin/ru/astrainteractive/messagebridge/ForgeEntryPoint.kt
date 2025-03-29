package ru.astrainteractive.messagebridge

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.fml.common.Mod
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.command.giveItemCommand
import ru.astrainteractive.messagebridge.di.RootModuleImpl
import ru.astrainteractive.messagebridge.forge.core.event.flowEvent
import javax.annotation.ParametersAreNonnullByDefault

@Mod("messagebridge")
@ParametersAreNonnullByDefault
class ForgeEntryPoint :
    Logger by JUtiltLogger("ForgeEntryPoint"),
    Lifecycle {
    private val rootModule by lazy { RootModuleImpl() }

    override fun onEnable() {
        rootModule.lifecycle.onEnable()
    }

    override fun onDisable() {
        info { "#onDisable" }
        rootModule.lifecycle.onDisable()
    }

    override fun onReload() {
        rootModule.lifecycle.onReload()
    }

    val serverStartedEvent = flowEvent<ServerStartedEvent>(EventPriority.HIGHEST)
        .onEach {
            info { "#serverStartedEvent" }
            onEnable()
        }.launchIn(rootModule.coreModule.scope)

    val serverStoppingEvent = flowEvent<ServerStoppingEvent>(EventPriority.HIGHEST)
        .onEach {
            info { "#serverStoppingEvent" }
            onDisable()
        }.launchIn(rootModule.coreModule.scope)

    val registerCommandsEvent = flowEvent<RegisterCommandsEvent>(EventPriority.HIGHEST)
        .onEach { e ->
            info { "#registerCommandsEvent" }
            e.giveItemCommand()
        }.launchIn(rootModule.coreModule.scope)
}
