package ru.astrainteractive.messagebridge.core.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import ru.astrainteractive.messagebridge.core.util.fabricEventFlow
import ru.astrainteractive.messagebridge.core.util.send

object FabricServer {
    val serverFlow = fabricEventFlow {
        val callback = ServerTickEvents.StartTick(::send)
        ServerTickEvents.START_SERVER_TICK.register(
            callback
        )
    }.map { it.t }.stateIn(GlobalScope, SharingStarted.Companion.Eagerly, null)

    fun getOnlinePlayers(): List<ServerPlayerEntity> {
        return serverFlow.value?.playerManager?.playerList.orEmpty().filterNotNull()
    }
}
