package ru.astrainteractive.messagebridge.forge.core.api

import net.luckperms.api.LuckPerms
import ru.astrainteractive.messagebridge.core.api.LuckPermsProvider

object ForgeLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return runCatching {
            net.luckperms.api.LuckPermsProvider.get()
        }.getOrNull()
    }
}
