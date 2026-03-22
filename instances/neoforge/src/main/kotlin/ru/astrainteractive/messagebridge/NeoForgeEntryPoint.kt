package ru.astrainteractive.messagebridge

import net.neoforged.fml.common.Mod
import ru.astrainteractive.astralibs.lifecycle.ForgeLifecycleServer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.mikro.core.logging.JUtiltLogger
import ru.astrainteractive.klibs.mikro.core.logging.Logger
import ru.astrainteractive.messagebridge.di.RootModule
import javax.annotation.ParametersAreNonnullByDefault

@Mod("messagebridge")
@ParametersAreNonnullByDefault
class NeoForgeEntryPoint :
    ForgeLifecycleServer(),
    Logger by JUtiltLogger("NeoForgeEntryPoint"),
    Lifecycle {
    private val rootModule by lazy { RootModule() }

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
