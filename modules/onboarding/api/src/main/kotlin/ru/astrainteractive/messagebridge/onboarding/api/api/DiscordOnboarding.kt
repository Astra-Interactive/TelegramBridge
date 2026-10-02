package ru.astrainteractive.messagebridge.onboarding.api.api

interface DiscordOnboarding : MessengerOnboarding {
    fun inviteUrl(): String?
}
