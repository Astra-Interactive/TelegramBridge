package ru.astrainteractive.messagebridge.onboarding.discord.internal

import net.dv8tion.jda.api.Permission

internal object DiscordPermissions {
    val CHANNEL: List<Permission> = listOf(
        Permission.VIEW_CHANNEL,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MANAGE_WEBHOOKS,
        Permission.MANAGE_CHANNEL,
    )

    val LINK: List<Permission> = listOf(Permission.MANAGE_ROLES)

    val ALL: List<Permission> = CHANNEL + LINK
}
