package ru.astrainteractive.messagebridge

import net.neoforged.fml.common.Mod
import ru.astrainteractive.astralibs.lifecycle.ForgeLifecycleServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.di.RootModuleImpl
import javax.annotation.ParametersAreNonnullByDefault

@Mod("messagebridge")
@ParametersAreNonnullByDefault
class NeoForgeEntryPoint :
    ForgeLifecycleServer(),
    Logger by JUtiltLogger("NeoForgeEntryPoint").withoutParentHandlers(),
    Lifecycle {
    private val rootModule by lazy { RootModuleImpl() }

    override fun onEnable() {
        rootModule.lifecycle.onEnable()
    }

    override fun onDisable() {
        rootModule.lifecycle.onDisable()
    }

    override fun onReload() {
        rootModule.lifecycle.onReload()
    }
}
