package ru.astrainteractive.messagebridge.core.bukkit.impl

import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import ru.astrainteractive.messagebridge.core.api.api.LuckPermsProvider

object BukkitLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return Bukkit.getServicesManager()
            .getRegistration(LuckPerms::class.java)
            ?.provider
    }
}
