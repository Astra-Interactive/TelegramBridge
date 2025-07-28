package ru.astrainteractive.messagebridge.core.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.network.ServerPlayerEntity
import ru.astrainteractive.astralibs.logging.JUtiltLogger
import ru.astrainteractive.astralibs.logging.Logger
import ru.astrainteractive.messagebridge.core.util.fabricEventFlow
import ru.astrainteractive.messagebridge.core.util.send

object FabricServer : Logger by JUtiltLogger("MessageBridge-FabricServer") {
    val serverFlow = fabricEventFlow {
        val callback = ServerLifecycleEvents.ServerStarted(::send)
        ServerLifecycleEvents.SERVER_STARTED.register(callback)
    }.map { it.t }.stateIn(GlobalScope, SharingStarted.Companion.Eagerly, null)

    fun getOnlinePlayers(): List<ServerPlayerEntity> {
        return serverFlow.value?.playerManager?.playerList.orEmpty().filterNotNull()
    }
}
