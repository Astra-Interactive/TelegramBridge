package ru.astrainteractive.messagebridge.forge.core.api

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import ru.astrainteractive.astralibs.server.util.ForgeUtil
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider

class ForgeOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return ForgeUtil.serverOrNull
            ?.playerList
            ?.players
            .orEmpty()
            .map(ServerPlayer::getName)
            .map(Component::getString)
    }
}
