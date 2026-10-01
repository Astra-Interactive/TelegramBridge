package ru.astrainteractive.messagebridge.core.bukkit.impl

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.astrainteractive.messagebridge.core.api.api.OnlinePlayersProvider

object BukkitOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return Bukkit.getOnlinePlayers().map(Player::getDisplayName)
    }
}
