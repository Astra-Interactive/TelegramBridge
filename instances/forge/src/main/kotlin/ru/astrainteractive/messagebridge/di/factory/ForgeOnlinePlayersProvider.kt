package ru.astrainteractive.messagebridge.di.factory

import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import ru.astrainteractive.messagebridge.OnlinePlayersProvider

class ForgeOnlinePlayersProvider(private val getServer: () -> MinecraftServer?) : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return getServer.invoke()
            ?.playerList
            ?.players
            .orEmpty()
            .map(ServerPlayer::getName)
            .map(Component::getString)
    }
}
