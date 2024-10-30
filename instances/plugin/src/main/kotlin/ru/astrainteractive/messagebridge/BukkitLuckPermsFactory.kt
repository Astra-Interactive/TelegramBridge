package ru.astrainteractive.messagebridge

import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import ru.astrainteractive.messagebridge.link.controller.di.factory.LuckPermsFactory

object BukkitLuckPermsFactory : LuckPermsFactory {
    override fun provide(): LuckPerms? {
        return Bukkit.getServicesManager()
            .getRegistration(LuckPerms::class.java)
            ?.provider
    }
}
