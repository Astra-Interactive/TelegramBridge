package ru.astrainteractive.messagebridge.core.forge.impl

import net.luckperms.api.LuckPerms
import ru.astrainteractive.messagebridge.core.api.api.LuckPermsProvider

object ForgeLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return runCatching {
            net.luckperms.api.LuckPermsProvider.get()
        }.getOrNull()
    }
}
