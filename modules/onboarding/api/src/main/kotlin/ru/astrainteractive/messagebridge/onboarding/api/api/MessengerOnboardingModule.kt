package ru.astrainteractive.messagebridge.onboarding.api.api

interface MessengerOnboardingModule<T : MessengerOnboarding> {
    val onboarding: T
}
