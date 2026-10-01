package ru.astrainteractive.messagebridge.messenger.discord.model

import net.dv8tion.jda.api.Permission

internal object DiscordPermissions {
    /** Reading and sending messages, posting as players through a webhook and the online count in the topic. */
    val CHANNEL: List<Permission> = listOf(
        Permission.VIEW_CHANNEL,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MANAGE_WEBHOOKS,
        Permission.MANAGE_CHANNEL,
    )

    /** Giving the link role to players who linked their account. */
    val LINK: List<Permission> = listOf(Permission.MANAGE_ROLES)

    val ALL: List<Permission> = CHANNEL + LINK
}
