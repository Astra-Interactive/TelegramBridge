package ru.astrainteractive.messagebridge.messenger.discord.model

sealed interface DiscordRoleChange {
    val discordUserId: Long
    val roleId: Long

    data class Grant(override val discordUserId: Long, override val roleId: Long) : DiscordRoleChange
}
