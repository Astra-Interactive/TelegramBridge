package ru.astrainteractive.messagebridge

import org.bukkit.plugin.java.JavaPlugin
import ru.astrainteractive.messagebridge.di.RootModuleImpl

class MessageBridge : JavaPlugin() {
    private val rootModule = RootModuleImpl(this)

    override fun onEnable() {
        rootModule.lifecycle.onEnable()
    }

    override fun onDisable() {
        rootModule.lifecycle.onDisable()
    }

    fun onReload() {
        rootModule.lifecycle.onReload()
    }
}
