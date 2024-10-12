package ru.astrainteractive.messagebridge.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.discordbot.module.bridge.di.ClientBridgeModule
import ru.astrainteractive.messagebridge.MessageBridgePlugin
import ru.astrainteractive.messagebridge.commands.di.CommandModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.events.di.EventModule
import ru.astrainteractive.messagebridge.messaging.di.MessagingModule

class RootModuleImpl(
    plugin: MessageBridgePlugin
) : Logger by JUtiltLogger("MessageBridge-RootModuleImpl") {
    val coreModule = CoreModule(plugin)

    val clientBridgeModule = ClientBridgeModule.Default()

    val telegramModule = TelegramModule(
        coreModule = coreModule,
        clientBridgeModule = clientBridgeModule
    )

    val messagingModule = MessagingModule(
        coreModule = coreModule,
        telegramModule = telegramModule
    )

    val eventModule = EventModule(
        coreModule = coreModule,
        messagingModule = messagingModule,
        clientBridgeModule = clientBridgeModule
    )

    val commandModule by lazy {
        CommandModule(coreModule)
    }

    private val lifecycles: List<Lifecycle>
        get() = listOf(
            coreModule.lifecycle,
            clientBridgeModule.lifecycle,
            eventModule.lifecycle,
            telegramModule.lifecycle,
            commandModule.lifecycle
        )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            lifecycles.forEach(Lifecycle::onEnable)
        },
        onReload = {
            lifecycles.forEach(Lifecycle::onReload)
        },
        onDisable = {
            lifecycles.forEach(Lifecycle::onDisable)
        }
    )
}
