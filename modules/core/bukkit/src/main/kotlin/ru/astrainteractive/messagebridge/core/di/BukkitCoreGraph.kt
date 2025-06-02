package ru.astrainteractive.messagebridge.core.di

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import ru.astrainteractive.klibs.mikro.core.dispatchers.KotlinDispatchers
import ru.astrainteractive.messagebridge.core.LifecyclePlugin
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider
import java.io.File

@DependencyGraph
interface BukkitCoreGraph : CoreGraph {
    val plugin: LifecyclePlugin

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides
            plugin: LifecyclePlugin,
            @Provides
            dispatchers: KotlinDispatchers,
            @Provides
            dataFolder: File,
            @Provides
            luckPermsProvider: LuckPermsProvider,
            @Provides
            onlinePlayersProvider: OnlinePlayersProvider
        )
    }
}