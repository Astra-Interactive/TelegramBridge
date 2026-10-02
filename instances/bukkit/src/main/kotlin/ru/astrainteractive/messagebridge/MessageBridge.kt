package ru.astrainteractive.messagebridge

import ru.astrainteractive.astralibs.lifecycle.LifecyclePlugin
import ru.astrainteractive.messagebridge.di.RootModule

class MessageBridge : LifecyclePlugin() {
    private val rootModule = RootModule(this)

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
