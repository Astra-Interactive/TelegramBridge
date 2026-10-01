package ru.astrainteractive.messagebridge.onboarding.fake

import ru.astrainteractive.messagebridge.onboarding.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.MessengerOnboardingModule

internal class FakeOnboardingModule<T : MessengerOnboarding>(
    override val onboarding: T
) : MessengerOnboardingModule<T>
