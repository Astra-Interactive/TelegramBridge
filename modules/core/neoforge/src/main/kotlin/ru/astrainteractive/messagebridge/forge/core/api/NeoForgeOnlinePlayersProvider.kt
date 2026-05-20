package ru.astrainteractive.messagebridge.forge.core.api

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import ru.astrainteractive.astralibs.server.util.MinecraftUtil
import ru.astrainteractive.messagebridge.core.api.OnlinePlayersProvider

class NeoForgeOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return MinecraftUtil.serverOrNull
            ?.playerList
            ?.players
            .orEmpty()
            .map(ServerPlayer::getName)
            .map(Component::getString)
    }
}
