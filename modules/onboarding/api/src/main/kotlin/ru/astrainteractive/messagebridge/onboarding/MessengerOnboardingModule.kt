package ru.astrainteractive.messagebridge.onboarding

/** A messenger module that the /mb commands can set up. */
interface MessengerOnboardingModule<T : MessengerOnboarding> {
    val onboarding: T
}
