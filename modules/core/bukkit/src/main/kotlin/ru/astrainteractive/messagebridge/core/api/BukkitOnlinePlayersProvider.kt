package ru.astrainteractive.messagebridge.core.api

import org.bukkit.Bukkit
import org.bukkit.entity.Player

object BukkitOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return Bukkit.getOnlinePlayers().map(Player::getDisplayName)
    }
}
