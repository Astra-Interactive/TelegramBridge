package ru.astrainteractive.messagebridge.di.factory

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import ru.astrainteractive.messagebridge.OnlinePlayersProvider

class ForgeOnlinePlayersProvider(private val serverStateFlow: StateFlow<MinecraftServer?>) : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return serverStateFlow.value
            ?.playerList
            ?.players
            .orEmpty()
            .map(ServerPlayer::getName)
            .map(Component::getString)
    }
}
