package ru.astrainteractive.messagebridge.onboarding.api.fake

import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.api.MessengerOnboardingModule

class FakeOnboardingModule<T : MessengerOnboarding>(
    override val onboarding: T
) : MessengerOnboardingModule<T>
