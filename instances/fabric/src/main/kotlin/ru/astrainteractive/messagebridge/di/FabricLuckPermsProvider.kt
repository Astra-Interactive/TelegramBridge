package ru.astrainteractive.messagebridge.di

import net.luckperms.api.LuckPerms
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider

object FabricLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return runCatching {
            net.luckperms.api.LuckPermsProvider.get()
        }.getOrNull()
    }
}