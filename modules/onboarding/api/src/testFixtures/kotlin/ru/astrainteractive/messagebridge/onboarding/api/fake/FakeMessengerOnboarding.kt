package ru.astrainteractive.messagebridge.onboarding.api.fake

import kotlinx.coroutines.flow.MutableStateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.api.api.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.api.model.BindCode
import ru.astrainteractive.messagebridge.onboarding.api.model.Check
import ru.astrainteractive.messagebridge.onboarding.api.model.MessengerStatus
import kotlin.time.Duration.Companion.minutes

class FakeMessengerOnboarding : DiscordOnboarding {
    override val status = MutableStateFlow<MessengerStatus>(MessengerStatus.Disabled)
    override val deliveryError = MutableStateFlow<LocalizableComponent?>(null)

    var checks: List<Check> = emptyList()
    var inviteUrl: String? = null
    val bindCallbacks = mutableListOf<(LocalizableComponent) -> Unit>()

    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode {
        bindCallbacks.add(onBound)
        return BindCode(value = BIND_CODE, lifetime = 10.minutes)
    }

    override suspend fun check(): List<Check> = checks

    override suspend fun inviteUrl(): String? = inviteUrl

    companion object {
        const val BIND_CODE = "48213705"
    }
}
