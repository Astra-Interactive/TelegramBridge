package ru.astrainteractive.messagebridge.core.api.api

import net.luckperms.api.LuckPerms

interface LuckPermsProvider {
    fun provide(): LuckPerms?
}
