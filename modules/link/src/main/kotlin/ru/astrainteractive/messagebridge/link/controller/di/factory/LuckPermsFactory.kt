package ru.astrainteractive.messagebridge.link.controller.di.factory

import net.luckperms.api.LuckPerms

interface LuckPermsFactory {
    fun provide(): LuckPerms?
}
