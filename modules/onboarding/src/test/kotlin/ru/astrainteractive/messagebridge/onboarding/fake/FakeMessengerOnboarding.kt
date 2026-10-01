package ru.astrainteractive.messagebridge.onboarding.fake

import kotlinx.coroutines.flow.MutableStateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.onboarding.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.bind.BindCode
import ru.astrainteractive.messagebridge.onboarding.check.Check
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import kotlin.time.Duration.Companion.minutes

/** A bot whose status the test drives, as the messenger module would once config.yml changes. */
internal class FakeMessengerOnboarding : DiscordOnboarding {
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
