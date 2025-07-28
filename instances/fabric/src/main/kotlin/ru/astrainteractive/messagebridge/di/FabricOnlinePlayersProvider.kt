package ru.astrainteractive.messagebridge.di

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider

class FabricOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return FabricServer.getOnlinePlayers().map(ServerPlayerEntity::getDisplayName).map(Text::getString)
    }
}