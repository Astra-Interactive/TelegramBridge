package ru.astrainteractive.messagebridge.onboarding.api.permission

import ru.astrainteractive.astralibs.server.permission.Permission

sealed class OnboardingPermission(override val value: String) : Permission {
    data object Setup : OnboardingPermission("tbridge.setup")
}
