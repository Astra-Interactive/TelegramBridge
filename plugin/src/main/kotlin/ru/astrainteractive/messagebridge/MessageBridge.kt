package ru.astrainteractive.messagebridge

import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin
import ru.astrainteractive.messagebridge.di.RootModuleImpl
import ru.astrainteractive.messagebridge.messaging.model.MessageEvent

class MessageBridge : JavaPlugin() {
    private val rootModule = RootModuleImpl(this)

    override fun onEnable() {
        runBlocking {
            rootModule.bridgeModule.pluginBridgeApi.broadcastEvent(MessageEvent.ServerOpen)
        }
        rootModule.lifecycle.onEnable()
    }

    override fun onDisable() {
        runBlocking {
            rootModule.bridgeModule.pluginBridgeApi.broadcastEvent(MessageEvent.ServerClosed)
        }
        rootModule.lifecycle.onDisable()
    }

    fun onReload() {
        rootModule.lifecycle.onReload()
    }
}
