package ru.astrainteractive.messagebridge.core.api

import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit

object BukkitLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return Bukkit.getServicesManager()
            .getRegistration(LuckPerms::class.java)
            ?.provider
    }
}
