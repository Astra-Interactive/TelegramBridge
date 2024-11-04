package ru.astrainteractive.messagebridge.di.factory

import net.luckperms.api.LuckPerms
import ru.astrainteractive.messagebridge.link.controller.di.factory.LuckPermsProvider

object ForgeLuckPermsProvider : LuckPermsProvider {
    override fun provide(): LuckPerms? {
        return kotlin.runCatching {
            net.luckperms.api.LuckPermsProvider.get()
        }.getOrNull()
    }
}
