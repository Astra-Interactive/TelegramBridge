package ru.astrainteractive.messagebridge.commands.fake

import kotlinx.coroutines.flow.MutableStateFlow
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.messagebridge.messaging.setup.DiagnosticCheck
import ru.astrainteractive.messagebridge.messaging.setup.DiscordSetup
import ru.astrainteractive.messagebridge.messaging.setup.MessengerStatus

/** A bot whose status the test drives, as the messenger module would once config.yml changes. */
internal class FakeMessengerSetup(
    status: MessengerStatus = MessengerStatus.Disabled,
    private val bindCode: String = "48213705"
) : DiscordSetup {
    override val status = MutableStateFlow(status)
    override val deliveryError = MutableStateFlow<LocalizableComponent?>(null)

    var checks: List<DiagnosticCheck> = emptyList()
    var inviteUrl: String? = null
    val bindCallbacks = mutableListOf<(LocalizableComponent) -> Unit>()

    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): String {
        bindCallbacks.add(onBound)
        return bindCode
    }

    override suspend fun diagnose(): List<DiagnosticCheck> = checks

    override suspend fun inviteUrl(): String? = inviteUrl
}
