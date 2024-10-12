package ru.astrainteractive.messagebridge.discord.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.discordbot.module.bridge.di.ServerBridgeModule
import ru.astrainteractive.messagebridge.discord.core.di.JdaCoreModule
import ru.astrainteractive.messagebridge.discord.event.di.JdaEventModule

class RootModule {
    val jdaCoreModule = JdaCoreModule()
    val jdaModule = JdaModule(jdaCoreModule)
    val serverBridgeModule = ServerBridgeModule.Default()
    val jdaEventModule = JdaEventModule(
        serverBridgeModule = serverBridgeModule,
        jdaModule = jdaModule
    )
    private val lifecycles: List<Lifecycle>
        get() = listOf(
            serverBridgeModule.lifecycle,
            jdaEventModule.lifecycle
        )
    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            lifecycles.forEach(Lifecycle::onEnable)
        }
    )
}
