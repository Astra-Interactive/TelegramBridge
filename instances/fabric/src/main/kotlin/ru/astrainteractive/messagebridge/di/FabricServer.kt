package ru.astrainteractive.messagebridge.di

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import ru.astrainteractive.messagebridge.fabricEventFlow
import ru.astrainteractive.messagebridge.send

object FabricServer {
    val serverFlow = fabricEventFlow {
        val callback = ServerTickEvents.StartTick(::send)
        ServerTickEvents.START_SERVER_TICK.register(callback)
    }.map { it.t }.stateIn(GlobalScope, SharingStarted.Companion.Eagerly, null)

    fun getOnlinePlayers(): List<ServerPlayerEntity> {
        return serverFlow.value?.playerManager?.playerList.orEmpty().filterNotNull()
    }
}