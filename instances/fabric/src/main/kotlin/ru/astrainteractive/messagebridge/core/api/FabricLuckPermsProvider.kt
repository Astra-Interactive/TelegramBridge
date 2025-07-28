package ru.astrainteractive.messagebridge.core.api

import net.luckperms.api.LuckPerms

object FabricLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return runCatching {
            net.luckperms.api.LuckPermsProvider.get()
        }.getOrNull()
    }
}
