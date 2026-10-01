package ru.astrainteractive.messagebridge.messenger.discord.onboarding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.astrainteractive.astralibs.localization.component.LocalizableComponent
import ru.astrainteractive.klibs.kstorage.api.CachedKrate
import ru.astrainteractive.klibs.kstorage.api.getValue
import ru.astrainteractive.messagebridge.core.PluginTranslation
import ru.astrainteractive.messagebridge.messenger.discord.connection.DiscordConnection
import ru.astrainteractive.messagebridge.messenger.discord.connection.awaitJda
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordDeliveryError
import ru.astrainteractive.messagebridge.messenger.discord.failure.DiscordFailureMapper
import ru.astrainteractive.messagebridge.onboarding.DiscordOnboarding
import ru.astrainteractive.messagebridge.onboarding.bind.BindCode
import ru.astrainteractive.messagebridge.onboarding.bind.BindCodes
import ru.astrainteractive.messagebridge.onboarding.check.Check
import ru.astrainteractive.messagebridge.onboarding.status.MessengerStatus
import kotlin.time.Duration.Companion.seconds

internal class JdaDiscordOnboarding(
    private val connection: StateFlow<DiscordConnection>,
    private val failureMapper: DiscordFailureMapper,
    private val bindCodes: BindCodes,
    private val diagnostics: DiscordDiagnostics,
    delivery: DiscordDeliveryError,
    translationKrate: CachedKrate<PluginTranslation>,
    scope: CoroutineScope,
) : DiscordOnboarding {
    private val translation by translationKrate

    override val status: StateFlow<MessengerStatus> = connection
        .map(::toStatus)
        .stateIn(scope, SharingStarted.Eagerly, MessengerStatus.Connecting)

    override val deliveryError: StateFlow<LocalizableComponent?> = delivery.text

    private fun toStatus(connection: DiscordConnection): MessengerStatus = when (connection) {
        DiscordConnection.Disabled -> MessengerStatus.Disabled
        DiscordConnection.Connecting -> MessengerStatus.Connecting
        is DiscordConnection.Connected -> MessengerStatus.Connected(connection.jda.selfUser.name)
        is DiscordConnection.Failed -> MessengerStatus.Failed(
            reason = failureMapper.toText(connection.failure, translation.discord)
        )
    }

    override fun issueBindCode(onBound: (LocalizableComponent) -> Unit): BindCode = bindCodes.issue(onBound)

    override suspend fun check(): List<Check> = diagnostics.diagnose()

    override suspend fun inviteUrl(): String? {
        return connection.awaitJda(INVITE_CONNECTION_WAIT)?.getInviteUrl(DiscordPermissions.ALL)
    }

    private companion object {
        val INVITE_CONNECTION_WAIT = 10.seconds
    }
}
