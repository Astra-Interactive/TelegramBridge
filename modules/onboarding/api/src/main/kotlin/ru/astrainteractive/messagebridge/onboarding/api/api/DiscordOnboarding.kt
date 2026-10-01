package ru.astrainteractive.messagebridge.onboarding.api.api

interface DiscordOnboarding : MessengerOnboarding {
    suspend fun inviteUrl(): String?
}
