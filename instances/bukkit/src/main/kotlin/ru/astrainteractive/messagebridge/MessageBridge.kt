package ru.astrainteractive.messagebridge

import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.di.RootModuleImpl

class MessageBridge : LifecyclePlugin() {
    private val rootModule = RootModuleImpl(this)

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
