package ru.astrainteractive.messagebridge.onboarding.api.di

import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding

interface MessengerOnboardingModule<T : MessengerOnboarding> {
    val onboarding: T
}
