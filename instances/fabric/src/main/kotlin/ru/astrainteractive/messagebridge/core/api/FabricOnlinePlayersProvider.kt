package ru.astrainteractive.messagebridge.core.api

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import kotlin.collections.map

class FabricOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return FabricServer.getOnlinePlayers()
            .map(ServerPlayerEntity::getDisplayName)
            .map(Text::getString)
    }
}
