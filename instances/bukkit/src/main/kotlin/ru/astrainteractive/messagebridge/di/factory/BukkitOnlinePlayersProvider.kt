package ru.astrainteractive.messagebridge.di.factory

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.astrainteractive.messagebridge.OnlinePlayersProvider

object BukkitOnlinePlayersProvider : OnlinePlayersProvider {
    override fun provide(): List<String> {
        return Bukkit.getOnlinePlayers().map(Player::getDisplayName)
    }
}
