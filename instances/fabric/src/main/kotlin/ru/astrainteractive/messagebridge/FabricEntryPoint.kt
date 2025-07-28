package ru.astrainteractive.messagebridge

import net.fabricmc.api.ModInitializer
import ru.astrainteractive.messagebridge.di.RootModule

class FabricEntryPoint : ModInitializer {

    private val rootModule: RootModule by lazy {
        RootModule()
    }

    override fun onInitialize() {
        println("AstraTemplate: onInitialize")
        rootModule.lifecycle.onEnable()
    }

    fun reload() {
        rootModule.lifecycle.onReload()
    }
}
