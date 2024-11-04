package ru.astrainteractive.messagebridge

import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.di.RootModuleImpl
import ru.astrainteractive.messagebridge.event.core.ForgeEventBusListener
import javax.annotation.ParametersAreNonnullByDefault

@Mod("messagebridge")
@ParametersAreNonnullByDefault
class ForgeEntryPoint :
    ForgeEventBusListener,
    Logger by JUtiltLogger("ForgeEntryPoint"),
    Lifecycle {
    private val rootModule by lazy { RootModuleImpl() }

    override fun onEnable() {
        rootModule.lifecycle.onEnable()
    }

    override fun onDisable() {
        info { "#onDisable" }
        rootModule.minecraftServer = null
        rootModule.lifecycle.onDisable()
        (this as ForgeEventBusListener).unregister()
    }

    override fun onReload() {
        rootModule.lifecycle.onReload()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEnable(e: ServerStartedEvent) {
        rootModule.minecraftServer = e.server
        onEnable()
    }

    @Suppress("UnusedParameter")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onServerStopping(event: ServerStoppingEvent) {
        onDisable()
    }

    @Suppress("UnusedParameter")
    @SubscribeEvent
    fun onCommandRegister(e: RegisterCommandsEvent) {
        info { "#onCommandRegister" }
    }

    init {
        register()
    }
}
