package ru.astrainteractive.messagebridge.onboarding

interface DiscordOnboarding : MessengerOnboarding {
    /** @return link that adds the bot to a server with the permissions it needs, `null` until the bot connects */
    suspend fun inviteUrl(): String?
}
