package ru.astrainteractive.messagebridge.core.neoforge.impl

import net.luckperms.api.LuckPerms
import ru.astrainteractive.messagebridge.core.api.api.LuckPermsProvider

object NeoForgeLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return runCatching {
            net.luckperms.api.LuckPermsProvider.get()
        }.getOrNull()
    }
}
