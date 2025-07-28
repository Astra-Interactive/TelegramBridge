package ru.astrainteractive.messagebridge.core.api

import kotlinx.coroutines.flow.map
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
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
