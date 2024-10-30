package ru.astrainteractive.messagebridge.link.controller.di.factory

import net.luckperms.api.LuckPerms

interface LuckPermsProvider {
    fun provide(): LuckPerms?
}
