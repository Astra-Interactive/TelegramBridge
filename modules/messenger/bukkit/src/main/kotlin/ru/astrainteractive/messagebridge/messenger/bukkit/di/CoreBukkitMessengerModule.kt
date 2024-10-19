package ru.astrainteractive.messagebridge.messenger.bukkit.di

import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messenger.bukkit.messaging.MinecraftMessageController

class CoreBukkitMessengerModule(
    coreModule: CoreModule,
) {
    val minecraftMessageController: MessageController = MinecraftMessageController(
        kyoriKrate = coreModule.kyoriKrate,
        translationKrate = coreModule.translationKrate
    )
    val lifecycle = Lifecycle.Lambda()
}
