package ru.astrainteractive.messagebridge.messenger.bukkit.di

import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.Krate
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messaging.MessageController
import ru.astrainteractive.messagebridge.messenger.bukkit.messaging.MinecraftMessageController

class CoreBukkitMessengerModule(
    coreModule: CoreModule,
    kyoriKrate: Krate<KyoriComponentSerializer>,
    linkingDao: LinkingDao
) {
    val minecraftMessageController: MessageController = MinecraftMessageController(
        kyoriKrate = kyoriKrate,
        translationKrate = coreModule.translationKrate,
        linkingDao = linkingDao
    )
    val lifecycle = Lifecycle.Lambda()
}
