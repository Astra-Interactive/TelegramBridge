package ru.astrainteractive.messagebridge.onboarding.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingTranslation(
    @SerialName("setup")
    val setup: SetupTranslation = SetupTranslation(),
    @SerialName("telegram")
    val telegram: TelegramSetupTranslation = TelegramSetupTranslation(),
    @SerialName("discord")
    val discord: DiscordSetupTranslation = DiscordSetupTranslation()
)
