package ru.astrainteractive.messagebridge.link.api.di

import ru.astrainteractive.messagebridge.link.api.api.DiscordMembership
import ru.astrainteractive.messagebridge.link.api.api.LinkApi
import ru.astrainteractive.messagebridge.link.api.player.api.LinkingDao

interface LinkModule {
    val linkApi: LinkApi
    val linkingDao: LinkingDao
    val discordMembership: DiscordMembership
}
