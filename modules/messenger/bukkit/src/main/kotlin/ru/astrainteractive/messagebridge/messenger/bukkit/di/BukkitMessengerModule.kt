package ru.astrainteractive.messagebridge.messenger.bukkit.di

import kotlinx.coroutines.cancel
import org.bukkit.event.HandlerList
import ru.astrainteractive.astralibs.kyori.KyoriComponentSerializer
import ru.astrainteractive.astralibs.lifecycle.Lifecycle
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.messagebridge.core.di.BukkitCoreModule
import ru.astrainteractive.messagebridge.core.di.CoreModule
import ru.astrainteractive.messagebridge.link.database.dao.LinkingDao
import ru.astrainteractive.messagebridge.messenger.bukkit.events.BukkitEvent
import ru.astrainteractive.messagebridge.messenger.bukkit.messaging.MinecraftBEventConsumer

class BukkitMessengerModule(
    coreModule: CoreModule,
    bukkitCoreModule: BukkitCoreModule,
    kyoriKrate: CachedKrate<KyoriComponentSerializer>,
    linkingDao: LinkingDao
) {
    private val minecraftBEventConsumer = MinecraftBEventConsumer(
        kyoriKrate = kyoriKrate,
        translationKrate = coreModule.translationKrate,
        linkingDao = linkingDao,
        dispatchers = coreModule.dispatchers
    )

    private val bukkitEvent = BukkitEvent(
        configKrate = coreModule.configKrate,
        scope = coreModule.scope,
        dispatchers = coreModule.dispatchers,
    )

    val lifecycle = Lifecycle.Lambda(
        onEnable = {
            bukkitEvent.onEnable(bukkitCoreModule.plugin)
        },
        onDisable = {
            HandlerList.unregisterAll(bukkitCoreModule.plugin)
            bukkitEvent.onDisable()
            minecraftBEventConsumer.cancel()
        }
    )
}
