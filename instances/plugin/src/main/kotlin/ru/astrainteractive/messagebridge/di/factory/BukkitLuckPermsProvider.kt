package ru.astrainteractive.messagebridge.di.factory

import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import ru.astrainteractive.messagebridge.link.controller.di.factory.LuckPermsProvider

object BukkitLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return Bukkit.getServicesManager()
            .getRegistration(LuckPerms::class.java)
            ?.provider
    }
}
