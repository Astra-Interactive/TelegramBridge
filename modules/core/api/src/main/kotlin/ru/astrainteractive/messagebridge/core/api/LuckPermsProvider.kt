package ru.astrainteractive.messagebridge.core.api

import net.luckperms.api.LuckPerms

interface LuckPermsProvider {
    fun provide(): LuckPerms?
}
